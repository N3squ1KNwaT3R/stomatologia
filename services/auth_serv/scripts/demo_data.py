"""Демо-данные для защиты: персонал, услуги с ценами, профили и расписание врачей, пациенты.

Запуск (после того как recp_serv применил свои миграции, т.е. хотя бы раз стартовал):
    docker compose exec auth_serv python -m scripts.demo_data

Скрипт идемпотентный: повторный запуск ничего не дублирует.
Пароль у всех демо-аккаунтов — password123 (только для разработки!).

Таблицы services / doctors / doctor_services / doctor_working_hours / patients принадлежат recp_serv.
Пишем в них напрямую только здесь, в dev-скрипте для наполнения пустой БД, — сервисный код так не делает.
"""
from datetime import date

from sqlalchemy import Connection, select, text

from app.db import SessionLocal
from app.models import Role, User
from app.security import hash_password
from scripts.seed import DEV_PASSWORD
from scripts.seed import main as seed_staff

# name, description, price (PLN), duration_min, buffer_min
SERVICES = [
    ("Konsultacja stomatologiczna", "Przegląd, plan leczenia", 150, 30, 10),
    ("Higienizacja", "Skaling, piaskowanie, polerowanie", 300, 60, 10),
    ("Wypełnienie kompozytowe", "Leczenie próchnicy, jedna powierzchnia", 350, 45, 15),
    ("Leczenie kanałowe", "Jeden kanał, pod mikroskopem", 900, 90, 15),
    ("Ekstrakcja zęba", "Usunięcie zęba w znieczuleniu", 400, 30, 15),
    ("Wybielanie", "Wybielanie gabinetowe", 1200, 60, 10),
]

# email doctor -> (specialization, room, [service names])
DOCTORS = {
    "doctor1@clinic.local": ("Stomatolog zachowawczy", "101", [
        "Konsultacja stomatologiczna", "Wypełnienie kompozytowe", "Leczenie kanałowe", "Higienizacja",
    ]),
    "doctor2@clinic.local": ("Chirurg stomatologiczny", "102", [
        "Konsultacja stomatologiczna", "Ekstrakcja zęba", "Wybielanie",
    ]),
}

# Как в recp_serv: weekday = DayOfWeek % 7 → 0 = воскресенье, 1 = понедельник … 6 = суббота
WORKING_HOURS = {
    "doctor1@clinic.local": [(1, "08:00", "16:00"), (2, "08:00", "16:00"), (3, "12:00", "20:00"),
                             (4, "08:00", "16:00"), (5, "08:00", "14:00")],
    "doctor2@clinic.local": [(1, "12:00", "20:00"), (2, "12:00", "20:00"), (3, "08:00", "16:00"),
                             (4, "12:00", "20:00"), (6, "09:00", "13:00")],
}

# email (None = без аккаунта, карту завела рецепция), first, last, pesel, birth, phone, verified
PATIENTS = [
    ("piotr.zielinski@example.com", "Piotr", "Zieliński", "85031512345", date(1985, 3, 15), "+48 501 111 222", True),
    ("kasia.wojcik@example.com", "Katarzyna", "Wójcik", "92072054321", date(1992, 7, 20), "+48 502 333 444", True),
    ("tomek.lewandowski@example.com", "Tomasz", "Lewandowski", None, None, "+48 503 555 666", False),
    (None, "Halina", "Kamińska", "58110298765", date(1958, 11, 2), "+48 504 777 888", True),
]


def _upsert_patient_user(db, email: str, first: str, last: str, phone: str) -> int:
    user = db.scalar(select(User).where(User.email == email))
    if user is None:
        user = User(email=email, first_name=first, last_name=last, phone=phone, role=Role.PATIENT,
                    password_hash=hash_password(DEV_PASSWORD))
        db.add(user)
        db.flush()
        print(f"added user    {email} (PATIENT)")
    return user.id


def _user_id(conn: Connection, email: str) -> int:
    uid = conn.scalar(text("SELECT id FROM users WHERE email = :e"), {"e": email})
    if uid is None:
        raise SystemExit(f"нет пользователя {email} — сначала scripts.seed")
    return uid


def _services(conn: Connection) -> dict[str, int]:
    ids = {}
    for name, descr, price, dur, buf in SERVICES:
        sid = conn.scalar(text("SELECT id FROM services WHERE name = :n"), {"n": name})
        if sid is None:
            sid = conn.scalar(text(
                "INSERT INTO services (name, description, price, duration_min, buffer_min) "
                "VALUES (:n, :d, :p, :dur, :buf) RETURNING id"
            ), {"n": name, "d": descr, "p": price, "dur": dur, "buf": buf})
            print(f"added service {name} — {price} PLN")
        ids[name] = sid
    return ids


def _doctors(conn: Connection, service_ids: dict[str, int]) -> None:
    for email, (spec, room, services) in DOCTORS.items():
        uid = _user_id(conn, email)
        did = conn.scalar(text("SELECT id FROM doctors WHERE user_id = :u"), {"u": uid})
        if did is None:
            did = conn.scalar(text(
                "INSERT INTO doctors (user_id, specialization, room) VALUES (:u, :s, :r) RETURNING id"
            ), {"u": uid, "s": spec, "r": room})
            print(f"added doctor  {email} — {spec}, room {room}")
        for name in services:
            conn.execute(text(
                "INSERT INTO doctor_services (doctor_id, service_id) VALUES (:d, :s) ON CONFLICT DO NOTHING"
            ), {"d": did, "s": service_ids[name]})
        has_hours = conn.scalar(text("SELECT count(*) FROM doctor_working_hours WHERE doctor_id = :d"), {"d": did})
        if not has_hours:
            for weekday, start, end in WORKING_HOURS[email]:
                conn.execute(text(
                    "INSERT INTO doctor_working_hours (doctor_id, weekday, start_time, end_time) "
                    "VALUES (:d, :w, CAST(:s AS time), CAST(:e AS time))"
                ), {"d": did, "w": weekday, "s": start, "e": end})
            print(f"added hours   {email} — {len(WORKING_HOURS[email])} days")


def _patients(db, conn: Connection) -> None:
    for email, first, last, pesel, birth, phone, verified in PATIENTS:
        uid = _upsert_patient_user(db, email, first, last, phone) if email else None
        if uid is not None:
            exists = conn.scalar(text("SELECT id FROM patients WHERE user_id = :u"), {"u": uid})
        else:
            exists = conn.scalar(text("SELECT id FROM patients WHERE pesel = :p"), {"p": pesel})
        if exists:
            continue
        conn.execute(text(
            "INSERT INTO patients (first_name, last_name, pesel, date_of_birth, phone, email, user_id, is_verified) "
            "VALUES (:f, :l, :pesel, :b, :ph, :e, :u, :v)"
        ), {"f": first, "l": last, "pesel": pesel, "b": birth, "ph": phone, "e": email, "u": uid, "v": verified})
        print(f"added patient {first} {last} ({'с аккаунтом' if uid else 'без аккаунта'}, "
              f"{'верифицирован' if verified else 'не верифицирован'})")


def main() -> None:
    seed_staff()
    with SessionLocal() as db:
        conn = db.connection()
        if conn.scalar(text("SELECT to_regclass('public.services')")) is None:
            raise SystemExit("таблиц recp_serv нет — сначала запустите recp_serv (Flyway создаст таблицы)")
        service_ids = _services(conn)
        _doctors(conn, service_ids)
        _patients(db, conn)
        db.commit()
    print("demo data ok")


if __name__ == "__main__":
    main()

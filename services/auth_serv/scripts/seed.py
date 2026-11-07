"""Стартовые аккаунты персонала. Запуск из папки сервиса:  python -m scripts.seed

Пароли — только для разработки!
"""
from sqlalchemy import select

from app.db import SessionLocal
from app.models import Role, User
from app.security import hash_password

STAFF = [
    ("admin@clinic.local", "Admin", "Clinic", Role.ADMIN),
    ("recp@clinic.local", "Anna", "Nowak", Role.RECEPTIONIST),
    ("doctor1@clinic.local", "Jan", "Kowalski", Role.DOCTOR),
    ("doctor2@clinic.local", "Maria", "Wisniewska", Role.DOCTOR),
]
DEV_PASSWORD = "password123"


def main() -> None:
    with SessionLocal() as db:
        for email, first, last, role in STAFF:
            if db.scalar(select(User).where(User.email == email)):
                print(f"skip  {email}")
                continue
            db.add(User(email=email, first_name=first, last_name=last, role=role,
                        password_hash=hash_password(DEV_PASSWORD)))
            print(f"added {email} ({role})")
        db.commit()


if __name__ == "__main__":
    main()

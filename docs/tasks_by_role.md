# Разбивка задач по сервисам — клиника (MVP)

Три сервиса из схемы → три разработчика. Ниже: кто за что отвечает, какие таблицы и эндпоинты делает, что отдаёт другим и в каком порядке работать.

> Стек (предположение, замените при необходимости): Python 3.12, FastAPI, SQLAlchemy 2 + Alembic, PostgreSQL, pytest. Для MVP рекомендуется **один репозиторий и одна БД, три модуля/роутера** (`auth`, `recp`, `user`), а не три отдельных деплоя — иначе вы утонете в межсервисных вызовах. Границы модулей при этом соблюдаются так же строго.

Структура репозитория:

```
app/
  core/          # config, db session, security (Dev A владеет)
  auth/          # Dev A
  recp/          # Dev B
  user/          # Dev C
  main.py
alembic/
tests/
```

## Кто за что отвечает

| | Dev A | Dev B | Dev C |
|---|---|---|---|
| Сервис | `auth_serv` | `recp_serv` | `user_serv` |
| Акторы | все (вход/регистрация) | recp, manager | patient, doctor, admin |
| Таблицы | `users`, `doctor_profiles`, `services`, `doctor_services` | `patients`*, `appointments`, `appointment_requests` | `visits`, `performed_services`, `tooth_records`, `patient_allergies`, `patient_chronic_conditions` |

\* `patients` создаётся при регистрации (Dev A вызывает функцию Dev B) — см. «Контракты».

Отличия от схемы draw.io: каталог услуг (`services`, `doctor_services`) отдан Dev A, потому что у него самая лёгкая нагрузка; аналитика админа — в конце Dev C. «Visit» из схемы = `appointment` (запись) в нашей БД.

---

## Dev A — `auth_serv` (аккаунты, доступ, каталог)

Он же делает **фундамент**, без которого остальные не могут начать: в первый день нужен каркас проекта.

### Фаза 0 — каркас (день 1, блокирует остальных)
- [ ] Структура проекта, `core/config.py` (.env), подключение к БД, Alembic
- [ ] Docker-compose: Postgres + приложение
- [ ] Модель и миграция `users` (поля из `db_schema.md`)
- [ ] Зависимости для всех: `get_current_user()` и `require_role(*roles)` — остальные вешают их на свои эндпоинты
- [ ] Заглушка `get_current_user`, чтобы B и C могли работать до готового логина

### Фаза 1 — аутентификация
- [ ] `POST /auth/register` — регистрация пациента: `email, first_name, last_name, pesel, phone, password`. Валидация (email, PESEL 11 цифр, пароль). Хеш пароля (argon2/bcrypt). Создаёт `users` (role=PATIENT) **и** пустую карту пациента через функцию Dev B (`create_patient_for_user`)
- [ ] `POST /auth/login` — проверка пароля → `200` + `Set-Cookie: access_token` (httpOnly, SameSite=Lax, Secure на проде). Cookie с JWT, срок ~1 день
- [ ] `POST /auth/logout` — очистка cookie
- [ ] `GET /auth/me` — текущий пользователь и роль
- [ ] `POST /auth/password-reset/request` и `/confirm` — токен на email (отправка письма — через простую функцию-заглушку `send_email`, SMTP потом)
- [ ] Защита от перебора: общий ответ на неверный логин/пароль

### Фаза 2 — каталог и персонал
- [ ] Модели и миграции `doctor_profiles`, `services`, `doctor_services`
- [ ] `GET /services` (публично, только `is_active`), `GET /services/{id}/doctors` — врачи, оказывающие услугу
- [ ] CRUD `/services` — роли RECEPTIONIST, ADMIN
- [ ] `POST/DELETE /doctors/{id}/services/{service_id}` — привязка услуги к врачу (RECEPTIONIST, ADMIN)
- [ ] Создание врачей и рецепционистов: `POST /staff` (ADMIN) — временно «костыль»: **seed-скрипт** `scripts/create_staff.py` + стартовые данные (услуги, пара врачей, рецепционист, админ)

### Тесты
- регистрация/логин/доступ по ролям, неверный пароль, дубликат email/PESEL

**Отдаёт другим:** `get_current_user`, `require_role`, модель `User`, seed-данные, `GET /services`.

---

## Dev B — `recp_serv` (рецепция, записи, пациенты)

Самый нагруженный по бизнес-логике: именно здесь живёт расписание.

### Фаза 1 — пациенты
- [ ] Модель и миграция `patients` (включая `user_id`, `is_verified`)
- [ ] Функция `create_patient_for_user(user)` — для Dev A при регистрации
- [ ] `POST /patients` — рецепция создаёт карту без аккаунта
- [ ] `GET /patients?search=` — поиск по фамилии / PESEL / телефону
- [ ] `GET/PATCH /patients/{id}`
- [ ] `POST /patients/{id}/verify` — `is_verified = true`
- [ ] `POST /patients/{old_id}/merge/{new_id}` — слияние: перенос записей с новой карты на старую, **перенос аллергий и хронических заболеваний без дублей** (UNIQUE по `allergen` / `name`; при конфликте оставить запись старой карты), привязка `user_id`, удаление пустой карты (в одной транзакции)
- [ ] Поле `patients.no_known_allergies` — в модели и в `PATCH /patients/{id}`

### Фаза 2 — записи (ядро)
- [ ] Модель и миграция `appointments` + `EXCLUDE`-ограничение на пересечение (SQL из `db_schema.md`)
- [ ] **`appointment_service.py`** — единая бизнес-логика, которую используют и рецепция, и пациент (Dev C):
  - `get_free_slots(doctor_id, service_id, date)` — свободные окна с учётом `duration_min + buffer_min`
  - `create_appointment(patient_id, doctor_id, service_id, start_at, source, created_by, duration_min=None, buffer_min=None)` — проверяет, что врач оказывает услугу, считает `end_at` и `blocked_until`, ловит ошибку constraint → `409 Conflict`
  - `cancel_appointment(id, by_user)`
- [ ] `GET /appointments/slots?doctor_id=&service_id=&date=`
- [ ] `POST /appointments` — запись через рецепцию (`SCHEDULED`, можно задать кастомные длительность/буфер)
- [ ] `GET /appointments?doctor_id=&date_from=&date_to=&status=` — календарь
- [ ] `POST /appointments/{id}/confirm` — `PENDING → SCHEDULED` (онлайн-записи)
- [ ] `POST /appointments/{id}/cancel`, `PATCH /appointments/{id}` — перенос/изменение
- [ ] `POST /appointments/{id}/no-show`

### Фаза 3 — заявки
- [ ] `POST /appointment-requests` (публично, без аккаунта) и `GET/PATCH` для рецепции (`NEW → PROCESSED/REJECTED`, привязка созданной записи)

### Позже (не MVP): manager
- [ ] «CRUD members, manage shifts» из схемы — управление персоналом и сменами. Для MVP не делаем; расписание врача определяется календарём записей

### Тесты
- пересечение записей (в т.ч. буфер), конкурентное создание, отмена освобождает слот, кастомная длительность 268+45 мин, слияние карт (в т.ч. перенос аллергий/хронических без дублей)

**Отдаёт другим:** `appointment_service` (Dev C), `create_patient_for_user` (Dev A), модели `Patient`, `Appointment`.

---

## Dev C — `user_serv` (пациент, врач, админ)

### Фаза 1 — врач (самое важное для работы клиники)
- [ ] Модели и миграции `visits`, `performed_services`, `tooth_records`, `patient_allergies`, `patient_chronic_conditions`
- [ ] `GET /doctor/appointments?date=` — мои записи на день (только свои, `doctor_id` из `current_user`)
- [ ] `POST /visits` — создать визит по `appointment_id` (при этом `appointments.status = COMPLETED`; нельзя создать два визита на одну запись)
- [ ] `GET/PATCH /visits/{id}` — жалобы, диагноз, заключение, рекомендации (врач правит только свои)
- [ ] `POST /visits/{id}/performed-services`, `DELETE ...` — что реально сделано (цена копируется из `services.price` в момент добавления, можно переопределить)
- [ ] `GET /patients/{id}/history` — **хронология визитов** (от новых к старым): дата, врач, плановая услуга, жалобы, диагноз, заключение, рекомендации, выполненные услуги с зубами и ценами. Запрос — из `db_schema.md`, п. 3.3; пагинация (`limit/offset`). Доступ: DOCTOR, RECEPTIONIST
- [ ] **Аллергические реакции:** `GET /patients/{id}/allergies`, `POST /patients/{id}/allergies`, `PATCH/DELETE /patients/{id}/allergies/{allergy_id}` (DOCTOR, RECEPTIONIST). Валидация `severity` ∈ {MILD, MODERATE, SEVERE}, дубль аллергена → `409`. `PUT /patients/{id}/no-known-allergies` — явное «аллергий нет»
- [ ] **Хронические заболевания:** `GET/POST /patients/{id}/conditions`, `PATCH/DELETE /patients/{id}/conditions/{condition_id}` (DOCTOR, RECEPTIONIST); «снять с учёта» = `is_active=false`
- [ ] `GET /patients/{id}/medical-card` — сводка для врача и рецепции одним запросом: данные пациента, аллергии, активные хронические заболевания, зубная карта, последние 5 визитов, флаг «аллергологический анамнез не собран»
- [ ] В `GET /doctor/appointments` добавить для каждой записи `allergy_alert` (есть ли аллергия `SEVERE`) и `anamnesis_missing` — чтобы врач видел риск ещё до открытия карты
- [ ] `GET /patients/{id}/teeth`, `PUT /patients/{id}/teeth/{tooth_number}` — зубная карта (номер зуба по FDI 11–48 / 51–85 — валидация)
- [ ] «CRU info of actor» из схемы: врач может читать и править медицинскую информацию карты (`patients.notes`) — через эндпоинт, согласованный с Dev B

### Фаза 2 — пациент (личный кабинет)
- [ ] `GET/PATCH /me/profile` — свои контактные данные (телефон, email; PESEL после верификации не меняется)
- [ ] `GET /me/appointments` — мои записи
- [ ] `POST /me/appointments` — онлайн-запись: `patient_id` берётся **только** из аккаунта; вызывает `appointment_service.create_appointment(source=WEBSITE, status=PENDING)`, длительность/буфер из услуги
- [ ] `POST /me/appointments/{id}/cancel` — отмена только своих и только `PENDING/SCHEDULED`
- [ ] `GET /me/visits` — своя хронология визитов (тот же формат, что `/patients/{id}/history`), **только если `is_verified = true`**
- [ ] `GET /me/allergies`, `GET /me/conditions` — просмотр своих аллергий и хронических заболеваний (только чтение, только при `is_verified = true`)

### Фаза 3 — админ
- [ ] `GET /admin/analytics/revenue?date_from=&date_to=` — выручка по `performed_services` (по дням, по врачам, по услугам)
- [ ] `GET /admin/analytics/appointments` — число записей по статусам, no-show

### Тесты
- врач не видит чужие записи, пациент не видит чужую карту/записи, неверифицированный пациент не видит историю/аллергии/хронические, расчёт выручки, один визит на запись
- аллергии: дубль аллергена → 409, `SEVERE` попадает в `allergy_alert`, пациент не может править аллергии
- история визитов: порядок от новых к старым, отменённые/будущие записи не попадают, выполненные услуги подтягиваются с ценой на момент приёма

**Использует:** `require_role` (Dev A), `appointment_service` (Dev B).

---

## Контракты между разработчиками (договориться в первый день)

| Что | Кто пишет | Кто использует | Сигнатура |
|---|---|---|---|
| Авторизация | A | B, C | `Depends(get_current_user)`, `Depends(require_role("DOCTOR"))` |
| Создание карты при регистрации | B | A | `create_patient_for_user(db, user) -> Patient` |
| Свободные слоты | B | C, фронт | `get_free_slots(db, doctor_id, service_id, date) -> list[Slot]` |
| Создание записи | B | C | `create_appointment(db, ...) -> Appointment` (исключения: `SlotTakenError`, `DoctorDoesNotProvideServiceError`) |
| Отмена записи | B | C | `cancel_appointment(db, appointment_id, by_user)` |
| Каталог услуг | A | B, C | `GET /services`, модель `Service` |
| Модели аллергий и хронических заболеваний | C | B (для слияния карт) | `PatientAllergy`, `PatientChronicCondition` — B импортирует модели для переноса при merge |

Формат ошибок единый: `{"detail": "...", "code": "SLOT_TAKEN"}`; коды статусов — `401` не авторизован, `403` нет прав, `404`, `409` конфликт слота, `422` валидация.

## Порядок работы

| Этап | Dev A | Dev B | Dev C |
|---|---|---|---|
| 0 | Каркас, `users`, заглушка auth | Модели `patients`, `appointments`, `appointment_service` (интерфейс-заглушка) | Модели `visits`, `performed_services`, `tooth_records` |
| 1 | Register / login / me | CRUD пациентов, слоты, запись через рецепцию | Эндпоинты врача: визит, выполненные услуги, **аллергии, хронические заболевания** |
| 2 | Каталог, seed, сброс пароля | Подтверждение, отмена, календарь, слияние карт (с переносом аллергий/хронических) | Зубная карта, **хронология визитов, `medical-card`**, кабинет пациента, онлайн-запись |
| 3 | Помощь с тестами, deploy | Заявки с сайта | Аналитика админа |
| 4 | Интеграционные тесты и общий прогон сценариев: онлайн-запись → подтверждение → приём → выручка |||

## Definition of Done для каждого эндпоинта
- есть проверка роли и владения данными (чужое не прочитать)
- валидация входных данных (Pydantic)
- миграция Alembic применяется «с нуля»
- минимум по одному тесту на успешный и на запрещённый сценарий

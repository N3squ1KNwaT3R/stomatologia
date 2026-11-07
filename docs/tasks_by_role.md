# Кто что делает — MVP

Контракты между сервисами: `docs/contracts.md`. Схема БД (справочно): `docs/db_schema.md`.

| Кто | Что | Стек |
|---|---|---|
| **Лид** | фронт целиком, nginx, интеграция, демо | — |
| **Лёша** | `recp_serv` — пациенты, услуги, врачи, расписание, записи | Java, Spring Boot |
| **Дев 2** | `user_serv` — визиты, выполненные услуги, аллергии, выручка | Python, FastAPI |
| **Даня** | доделки `auth_serv`, Postman-коллекция, демо-данные | Python |

**Цель MVP — один сквозной сценарий, который работает на защите:**
регистрация → пациент записывается онлайн (PENDING) → рецепция подтверждает → врач проводит визит и вносит выполненные услуги → админ видит выручку.

Всё, что не помогает этому сценарию, — в самый конец или «в планах».

---

## ✅ Уже готово

- `auth_serv`: register / login / logout / me, JWT в cookie и Bearer, роли, миграция `users`, seed (админ, рецепция, 2 врача, пароль `password123`), тесты
- `docker-compose.yml` (Postgres, отдельная БД на каждый сервис), `.env.example`, контракты

---

## Лёша — `recp_serv` (Java)

Самая важная часть: от неё зависят и фронт, и `user_serv`.

### Этап 0 — каркас (первые дни)
- [ ] Spring Boot, `Dockerfile`, порт **8000** внутри контейнера, Flyway
- [ ] JWT-фильтр по `docs/contracts.md` → «Java: проверка токена» (Bearer **или** cookie `access_token`)
- [ ] Ошибки в формате `{"detail", "code"}`
- [ ] **Swagger (springdoc) с заглушками всех эндпоинтов ниже** — чтобы лид мог начать фронт, не дожидаясь логики

### Этап 1 — справочники и пациенты
- [ ] Таблицы: `patients`, `services`, `doctors` (`user_id`, ФИО, специализация, кабинет), `doctor_services`, `doctor_working_hours`
- [ ] `GET /services` (публично), `GET /services/{id}/doctors`
- [ ] CRUD `/services` — RECEPTIONIST, ADMIN
- [ ] `POST /doctors`, `PUT /doctors/{id}/services`, `PUT /doctors/{id}/working-hours` — ADMIN, RECEPTIONIST
- [ ] `POST /patients`, `GET /patients?search=` (фамилия / PESEL / телефон), `GET/PATCH /patients/{id}`, `POST /patients/{id}/verify` — RECEPTIONIST
- [ ] Внутренние: `POST /internal/patients` (вызывает auth при регистрации, идемпотентно по `user_id`), `GET /internal/patients/by-user/{user_id}`

### Этап 2 — записи (ядро)
- [ ] Таблица `appointments` + `EXCLUDE`-ограничение от пересечений (SQL в `db_schema.md`, раздел 6)
- [ ] `GET /appointments/slots?doctor_id=&service_id=&date=` — рабочие часы минус занятые записи, шаг 15 мин, `duration + buffer` целиком влезает в смену
- [ ] Рецепция: `POST /appointments` (SCHEDULED, можно свою длительность/буфер), `GET /appointments?doctor_id=&date_from=&date_to=&status=`, `POST /appointments/{id}/confirm`, `POST /appointments/{id}/cancel`
- [ ] Пациент: `GET /me/appointments`, `POST /me/appointments` (WEBSITE, PENDING, `patient_id` только из токена), `POST /me/appointments/{id}/cancel`
- [ ] Врач: `GET /doctor/appointments?date=` — только свои
- [ ] Внутренние для `user_serv`: `GET /internal/appointments/{id}`, `POST /internal/appointments/{id}/complete`, `GET /internal/services/{id}`

### Тесты
- [ ] пересечение записей (в т.ч. буфер) → 409, отмена освобождает слот, запись вне смены → 409, пациент не видит/не отменяет чужие записи

### Не делаем в MVP
слияние карт, заявки без аккаунта (`appointment_requests`), перенос записи, no-show

---

## Дев 2 — `user_serv` (Python)

Каркас уже лежит в `services/user_serv`: config, БД, Alembic, проверка JWT (`require_role`), формат ошибок, тесты с фикстурой `token()`.
Пока у Лёши нет внутренних эндпоинтов — клиент к `recp_serv` замокать.

### Этап 1 — визиты
- [ ] Таблицы: `visits` (+ снимок записи: `appointment_id` UNIQUE, `patient_id`, `doctor_user_id`, `start_at`, `service_name`), `performed_services`
- [ ] `POST /visits` по `appointment_id` — DOCTOR. Проверить через recp, что запись **этого врача** и в статусе `SCHEDULED`; после создания — `POST recp/internal/appointments/{id}/complete`. Второй визит на ту же запись → 409
- [ ] `GET/PATCH /visits/{id}` — жалобы, диагноз, заключение, рекомендации (врач правит только свои)
- [ ] `POST /visits/{id}/performed-services`, `DELETE .../{ps_id}` — цена берётся из recp в момент добавления, можно переопределить

### Этап 2 — медкарта
- [ ] Таблица `patient_allergies` (UNIQUE `patient_id, allergen`, `severity` ∈ MILD/MODERATE/SEVERE)
- [ ] `GET/POST /patients/{id}/allergies`, `DELETE /patients/{id}/allergies/{aid}` — DOCTOR, RECEPTIONIST; дубль → 409
- [ ] `GET /patients/{id}/history` — визиты от новых к старым с выполненными услугами — DOCTOR, RECEPTIONIST
- [ ] Пациент: `GET /me/visits`, `GET /me/allergies` — только если карта верифицирована (`GET recp/internal/patients/by-user/{user_id}`)

### Этап 3 — админ
- [ ] `GET /admin/revenue?date_from=&date_to=` — сумма `price * quantity`, плюс разбивка по дням

### Тесты
- [ ] врач не создаёт визит по чужой записи, второй визит → 409, пациент не видит чужое, неверифицированный не видит историю, расчёт выручки

### Если останется время
зубная карта (`tooth_records`), хронические заболевания

---

## Даня — `auth_serv` + демо

Сервис уже работает, задачи маленькие и ничего не блокируют.

- [ ] Разобраться в коде `auth_serv`, запустить тесты (`cd services/auth_serv && pytest`)
- [ ] `POST /staff` — ADMIN создаёт пользователя с ролью DOCTOR / RECEPTIONIST (по образцу `register`) + тест
- [ ] `POST /auth/change-password` (старый + новый пароль) + тест
- [ ] После регистрации вызывать `POST recp/internal/patients` (когда у Лёши будет готово). **Если к этапу 2 не сделано — забирает Дев 2**
- [ ] Postman/Bruno-коллекция всего сквозного сценария (лежит в репо, `docs/postman/`)
- [ ] Демо-данные: 5–6 услуг с ценами, расписание врачей, 3–4 пациента — скрипт или SQL
- [ ] ER-диаграмма для презентации

---

## Лид — фронт и интеграция

- [ ] nginx в compose: фронт + прокси `/api/auth`, `/api/recp`, `/api/user` → один адрес, cookie работает, CORS не нужен
- [ ] Публичное: список услуг, регистрация, логин
- [ ] Пациент: запись (услуга → врач → дата → слот), мои записи + отмена, мои визиты
- [ ] Рецепция: календарь, список PENDING с подтверждением, поиск/создание пациента, запись пациента
- [ ] Врач: записи на день, форма визита (жалобы, диагноз, выполненные услуги), аллергии пациента (плашка SEVERE)
- [ ] Админ: выручка за период
- [ ] Ревью PR, прогон сценария перед защитой

---

## Порядок

| Этап | Лёша | Дев 2 | Даня | Лид |
|---|---|---|---|---|
| 0 | каркас + Swagger с заглушками | модели `visits`, `performed_services`, клиент к recp (мок) | разобраться в auth, `POST /staff` | nginx, логин/регистрация |
| 1 | справочники, пациенты | визиты, выполненные услуги | смена пароля, вызов recp при регистрации | страницы пациента |
| 2 | слоты, записи | аллергии, история | демо-данные, Postman | рецепция, врач |
| 3 | тесты, багфикс | выручка, тесты | ER-диаграмма | админ, полировка |
| 4 | общий прогон сценария, фиксы ||||

## Definition of Done (для всех)

- проверка роли **и** владения (чужое не прочитать и не изменить)
- валидация входа, ошибки в формате `{"detail", "code"}`
- миграции применяются на чистой БД
- тест на успешный и на запрещённый сценарий
- код через PR, не пушить напрямую в `main`

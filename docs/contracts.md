# Контракты между сервисами

Этот файл — единственное, что нужно знать сервису о соседях.

`auth_serv` — Python (FastAPI). `recp_serv` и `user_serv` — Java (Spring Boot).

## Сервисы и порты

| Сервис | Порт снаружи | Внутри docker-сети | БД |
|---|---|---|---|
| auth_serv | 8001 | http://auth_serv:8000 | auth_db |
| recp_serv | 8002 | http://recp_serv:8000 | recp_db |
| user_serv | 8003 | http://user_serv:8000 | user_db |

Сервисы на Java внутри контейнера тоже слушают **8000**.

Один контейнер Postgres, **у каждого сервиса своя БД**. В чужую БД не ходим, только через HTTP.
Поэтому ссылки на чужие сущности (`user_id`, `doctor_id`) хранятся как обычный `bigint` **без FK**.

## Аутентификация (JWT)

- Токен выдаёт только `auth_serv` (`POST /auth/login`).
- Алгоритм **HS256**, секрет — переменная окружения `JWT_SECRET`, одинаковая во всех сервисах (≥ 32 байт).
- Payload:
  ```json
  {"sub": "42", "role": "DOCTOR", "iat": 1730980000, "exp": 1731066400}
  ```
  `sub` — id из `auth_db.users`, **строкой**. `role` ∈ `PATIENT | RECEPTIONIST | DOCTOR | ADMIN`.
- Токен принимается из cookie `access_token` (браузер) **или** из заголовка `Authorization: Bearer <token>` (Swagger, Postman, сервисы).
- Остальные сервисы только проверяют подпись и `exp`, в `auth_serv` за пользователем не ходят.

## Java: проверка токена

Секрет должен быть **≥ 32 байт**, иначе jjwt откажется работать с HS256 (`WeakKeyException`).

```java
// io.jsonwebtoken:jjwt-api / jjwt-impl / jjwt-jackson  0.12.x
SecretKey key = Keys.hmacShaKeyFor(System.getenv("JWT_SECRET").getBytes(StandardCharsets.UTF_8));

Claims c = Jwts.parser().verifyWith(key).build()
        .parseSignedClaims(token)      // бросит JwtException, если подпись/exp не ок -> 401
        .getPayload();
long userId = Long.parseLong(c.getSubject());
String role = c.get("role", String.class);
```

Токен брать из `Authorization: Bearer ...`, а если его нет — из cookie `access_token`.
Проще всего это сделать в своём `OncePerRequestFilter`, который кладёт в `SecurityContext`
пользователя с authority `ROLE_<role>`; дальше `@PreAuthorize("hasRole('DOCTOR')")`.
(Стандартный `oauth2-resource-server` cookie сам не читает — нужен свой `BearerTokenResolver`.)

## Формат ошибок (все сервисы)

```json
{"detail": "Человекочитаемое описание", "code": "SLOT_TAKEN"}
```

`401` нет/битый токен · `403` нет прав · `404` · `409` конфликт (слот занят, дубль) · `422` валидация.

## Внутренние вызовы сервис → сервис

Префикс `/internal/*`, заголовок `X-Internal-Token: $INTERNAL_TOKEN`. Наружу эти эндпоинты не отдаём.

| Кто вызывает | Кого | Зачем |
|---|---|---|
| auth_serv | `POST recp/internal/patients` `{user_id, first_name, last_name, email, phone}` | Создать пустую карту при регистрации. Идемпотентно по `user_id` |
| user_serv | `GET recp/internal/appointments/{id}` | Проверить, что запись этого врача и в статусе `SCHEDULED` |
| user_serv | `POST recp/internal/appointments/{id}/complete` | Пометить запись `COMPLETED` при создании визита |
| user_serv | `GET recp/internal/services/{id}` | Узнать цену услуги для `performed_services` |

> Визит в `user_serv` при создании сохраняет **снимок** нужных полей записи (`patient_id`, `doctor_id`,
> `start_at`, название услуги). Тогда история визитов и выручка считаются без похода в recp_serv.

## Кому какие таблицы (предложение, утвердить командой)

| Сервис | Таблицы |
|---|---|
| auth_serv | `users`, (позже) сброс пароля |
| recp_serv | `patients`, `services`, `doctors` (`user_id`, специализация, кабинет), `doctor_services`, `doctor_working_hours`, `appointments`, `appointment_requests` |
| user_serv | `visits`, `performed_services`, `tooth_records`, `patient_allergies`, `patient_chronic_conditions` |

Каталог услуг и врачи перенесены в recp_serv: без длительности услуги и рабочих часов врача нельзя
посчитать свободные слоты, а синхронный вызов в другой сервис на каждый расчёт слотов — лишняя хрупкость.

## Рабочие часы врача (`doctor_working_hours`, recp_serv)

| Поле | Тип | |
|---|---|---|
| id | bigserial | PK |
| doctor_id | bigint | FK → doctors.id |
| weekday | smallint | 0 = пн … 6 = вс |
| start_time | time | 08:00 |
| end_time | time | 14:00 |
| | | CHECK (start_time < end_time) |

Свободные слоты = рабочие интервалы врача на дату (таймзона клиники `Europe/Warsaw`)
минус `appointments` в статусах `PENDING/SCHEDULED` (по `[start_at, blocked_until)`), шаг сетки 15 мин.
Приём должен целиком (`duration + buffer`) помещаться в рабочий интервал.

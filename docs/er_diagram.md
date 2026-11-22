# ER-диаграмма (для презентации)

Одна БД `clinic`, таблицы поделены по сервисам-владельцам. Колонки — по реальным миграциям
(`auth_serv/alembic`, `recp_serv/db/migration` V1–V5); таблицы `user_serv` — по `docs/db_schema.md`
(сервис ещё в работе). Подробности полей — в `docs/db_schema.md`.

GitHub и VS Code (с Mermaid-плагином) рисуют диаграмму сами. Картинка для слайда:
вставить блок в https://mermaid.live → Actions → PNG/SVG.

```mermaid
erDiagram
    %% ---------- auth_serv ----------
    users {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar first_name
        varchar last_name
        varchar phone
        varchar role "PATIENT | RECEPTIONIST | DOCTOR | ADMIN"
        boolean is_active
        timestamptz created_at
    }

    %% ---------- recp_serv ----------
    patients {
        bigint id PK
        bigint user_id FK,UK "NULL — карта без аккаунта"
        varchar first_name
        varchar last_name
        varchar pesel UK
        date date_of_birth
        varchar phone
        varchar email
        text notes
        boolean is_verified
        timestamptz created_at
    }
    doctors {
        bigint id PK
        bigint user_id FK,UK
        varchar specialization
        varchar room
    }
    services {
        bigint id PK
        varchar name
        text description
        numeric price
        int duration_min
        int buffer_min
        boolean is_active
    }
    doctor_services {
        bigint doctor_id PK,FK
        bigint service_id PK,FK
    }
    doctor_working_hours {
        bigint id PK
        bigint doctor_id FK
        smallint weekday "0 = вс … 6 = сб"
        time start_time
        time end_time
    }
    appointments {
        bigint id PK
        bigint patient_id FK
        bigint doctor_id FK
        bigint service_id FK
        timestamptz start_at
        int duration_min
        int buffer_min
        timestamptz end_at
        timestamptz blocked_until "EXCLUDE: без пересечений у врача"
        varchar status "PENDING | SCHEDULED | COMPLETED | CANCELLED"
        varchar source "RECEPTION | WEBSITE"
        varchar room
        text comment
        bigint created_by FK
        timestamptz created_at
        timestamptz cancelled_at
    }

    %% ---------- user_serv ----------
    visits {
        bigint id PK
        bigint appointment_id FK,UK
        timestamptz started_at
        timestamptz finished_at
        text complaints
        text diagnosis
        text summary
        text recommendations
        timestamptz created_at
    }
    performed_services {
        bigint id PK
        bigint visit_id FK
        bigint service_id FK
        smallint tooth_number
        int quantity
        numeric price "снимок цены"
    }
    patient_allergies {
        bigint id PK
        bigint patient_id FK
        varchar allergen
        varchar reaction
        varchar severity "MILD | MODERATE | SEVERE"
        text notes
        bigint created_by FK
        timestamptz created_at
    }

    users ||--o| patients : "аккаунт пациента"
    users ||--o| doctors : "аккаунт врача"
    users ||--o{ appointments : "created_by"
    doctors ||--o{ doctor_services : ""
    services ||--o{ doctor_services : ""
    doctors ||--o{ doctor_working_hours : "смены"
    patients ||--o{ appointments : ""
    doctors ||--o{ appointments : ""
    services ||--o{ appointments : ""
    appointments ||--o| visits : "1 : 0..1"
    visits ||--o{ performed_services : ""
    services ||--o{ performed_services : ""
    patients ||--o{ patient_allergies : ""
    users ||--o{ patient_allergies : "created_by"
```

| Сервис | Таблицы |
|---|---|
| `auth_serv` (Python) | `users` |
| `recp_serv` (Java) | `patients`, `doctors`, `services`, `doctor_services`, `doctor_working_hours`, `appointments` |
| `user_serv` (Python) | `visits`, `performed_services`, `patient_allergies` |

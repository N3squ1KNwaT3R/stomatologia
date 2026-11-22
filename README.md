# Стоматология — MVP

Три сервиса: `auth_serv` (Python: аккаунты, JWT), `recp_serv` (Java: пациенты, расписание, записи),
`user_serv` (Python: визиты, медкарта, аналитика). Контракты между ними — `docs/contracts.md`.

## Запуск

```bash
cp .env.example .env          # поменять секреты
docker compose up --build db auth_serv user_serv   # пока recp (Java) нет; потом просто docker compose up --build
docker compose exec auth_serv python -m scripts.seed   # админ, рецепция, 2 врача (пароль password123)
docker compose exec auth_serv python -m scripts.demo_data   # + услуги, расписание врачей, пациенты (нужен поднятый recp_serv)
```

Сквозной сценарий в Postman: импортировать `docs/postman/clinic_mvp.postman_collection.json`, запустить папки по порядку.
ER-диаграмма: `docs/er_diagram.md`.

Swagger: http://localhost:8001/docs · http://localhost:8002/docs · http://localhost:8003/docs

## Разработка без Docker

```bash
python3.12 -m venv .venv && source .venv/bin/activate
pip install -r services/auth_serv/requirements.txt
cd services/auth_serv && pytest          # тесты на SQLite, Postgres не нужен
```

Новая миграция (из папки сервиса, при поднятой БД):

```bash
DATABASE_URL=postgresql+psycopg://clinic:clinic@localhost:5432/clinic alembic revision --autogenerate -m "patients"
```

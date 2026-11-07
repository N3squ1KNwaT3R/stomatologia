# user_serv — Java (Spring Boot)

- `Dockerfile` в этой папке, приложение слушает **8000** внутри контейнера
- БД из переменных `SPRING_DATASOURCE_URL / USERNAME / PASSWORD` (уже прописаны в docker-compose.yml)
- JWT проверять по `docs/contracts.md` → раздел «Java»
- миграции — Flyway или Liquibase, применяются при старте «с нуля»
- ошибки в формате `{"detail": "...", "code": "..."}`

# recp_serv — Java (Spring Boot)

- `Dockerfile` в этой папке, приложение слушает **8000** внутри контейнера
- БД `clinic` — общая с другими сервисами, из переменных `SPRING_DATASOURCE_URL / USERNAME / PASSWORD` (уже прописаны в docker-compose.yml)
- JWT проверять по `docs/contracts.md` → раздел «Java»
- миграции — Flyway или Liquibase, применяются при старте «с нуля»
- ошибки в формате `{"detail": "...", "code": "..."}`
- БД общая, в ней уже могут лежать чужие таблицы: у Flyway `spring.flyway.baseline-on-migrate=true`
  (или своя `spring.flyway.table=flyway_schema_history_recp`), иначе упадёт на «non-empty schema»

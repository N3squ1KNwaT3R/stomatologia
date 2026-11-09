"""Модели сервиса. Импортировать сюда все модели, чтобы их видел Alembic (alembic/env.py).

БД общая (`clinic`): ссылки на таблицы других сервисов (users, patients, appointments)
делаем обычным ForeignKey. Писать в чужие таблицы нельзя (см. docs/contracts.md).
"""
from app.db import Base  # noqa: F401

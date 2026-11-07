"""Модели сервиса. Импортировать сюда все модели, чтобы их видел Alembic (alembic/env.py).

FK только внутри своей БД. Ссылки на сущности других сервисов (patient_id, appointment_id,
doctor user_id) хранятся как обычный BigInteger без ForeignKey.
"""
from app.db import Base  # noqa: F401

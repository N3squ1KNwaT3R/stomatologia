from datetime import UTC, datetime, timedelta

import jwt
import pytest
from fastapi import Depends
from fastapi.testclient import TestClient

from app.auth import require_internal, require_role
from app.config import settings
from app.main import app


@app.get("/_test/doctor-only")
def _doctor_only(user=Depends(require_role("DOCTOR"))):
    return {"id": user.id}


@app.get("/_test/internal", dependencies=[Depends(require_internal)])
def _internal():
    return {"ok": True}


@pytest.fixture
def client():
    with TestClient(app) as c:
        yield c


@pytest.fixture
def token():
    """Выпускает JWT так же, как auth_serv — для тестов без поднятого auth."""
    def _make(user_id=1, role="PATIENT", ttl=timedelta(hours=1), secret=None):
        now = datetime.now(UTC)
        return jwt.encode({"sub": str(user_id), "role": role, "iat": now, "exp": now + ttl},
                          secret or settings.jwt_secret, algorithm=settings.jwt_algorithm)
    return _make

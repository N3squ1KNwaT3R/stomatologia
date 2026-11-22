import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.db import Base, get_db
from app.main import app
from app.models import User
from app.recp_client import get_recp_client
from app.security import hash_password


@pytest.fixture
def db():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    TestingSession = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)
    with TestingSession() as session:
        yield session


class FakeRecp:
    """Вместо настоящего recp_serv: запоминает, для кого создавали карту."""

    def __init__(self):
        self.created_for: list[int] = []

    def create_patient_card(self, user) -> bool:
        self.created_for.append(user.id)
        return True


@pytest.fixture
def recp():
    return FakeRecp()


@pytest.fixture
def client(db, recp):
    app.dependency_overrides[get_db] = lambda: db
    app.dependency_overrides[get_recp_client] = lambda: recp
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()


@pytest.fixture
def make_user(db):
    def _make(email="u@test.pl", password="password123", role="PATIENT"):
        user = User(email=email, password_hash=hash_password(password),
                    first_name="T", last_name="U", role=role)
        db.add(user)
        db.commit()
        return user
    return _make

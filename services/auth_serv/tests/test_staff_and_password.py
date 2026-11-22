import httpx

from app.recp_client import RecpClient

STAFF = {"email": "Dr@Clinic.pl", "password": "password123", "first_name": "Ewa", "last_name": "Lis",
         "role": "DOCTOR"}
REG = {"email": "jan@test.pl", "password": "password123", "first_name": "Jan", "last_name": "K"}


def login(client, email, password="password123"):
    return client.post("/auth/login", json={"email": email, "password": password})


# ---------- POST /auth/staff ----------

def test_admin_creates_doctor_and_receptionist(client, make_user):
    make_user("a@test.pl", role="ADMIN")
    login(client, "a@test.pl")
    r = client.post("/auth/staff", json=STAFF)
    assert r.status_code == 201
    assert r.json()["role"] == "DOCTOR"
    assert r.json()["email"] == "dr@clinic.pl"
    r = client.post("/auth/staff", json={**STAFF, "email": "recp@clinic.pl", "role": "RECEPTIONIST"})
    assert r.status_code == 201
    assert r.json()["role"] == "RECEPTIONIST"
    # новый сотрудник может войти
    assert login(client, "dr@clinic.pl").status_code == 200


def test_staff_forbidden_for_non_admin(client, make_user):
    for role in ("PATIENT", "RECEPTIONIST", "DOCTOR"):
        email = f"{role.lower()}@test.pl"
        make_user(email, role=role)
        assert login(client, email).status_code == 200
        assert client.post("/auth/staff", json=STAFF).status_code == 403


def test_staff_requires_auth(client):
    assert client.post("/auth/staff", json=STAFF).status_code == 401


def test_staff_cannot_create_admin_or_patient(client, make_user):
    make_user("a@test.pl", role="ADMIN")
    login(client, "a@test.pl")
    for role in ("ADMIN", "PATIENT", "nonsense"):
        assert client.post("/auth/staff", json={**STAFF, "role": role}).status_code == 422


def test_staff_duplicate_email(client, make_user):
    make_user("a@test.pl", role="ADMIN")
    login(client, "a@test.pl")
    client.post("/auth/staff", json=STAFF)
    r = client.post("/auth/staff", json={**STAFF, "email": "dr@clinic.pl"})
    assert r.status_code == 409
    assert r.json()["code"] == "EMAIL_TAKEN"


def test_staff_does_not_create_patient_card(client, make_user, recp):
    make_user("a@test.pl", role="ADMIN")
    login(client, "a@test.pl")
    client.post("/auth/staff", json=STAFF)
    assert recp.created_for == []


# ---------- POST /auth/change-password ----------

def test_change_password(client, make_user):
    make_user("u@test.pl")
    login(client, "u@test.pl")
    r = client.post("/auth/change-password", json={"old_password": "password123", "new_password": "newpass456"})
    assert r.status_code == 204
    assert login(client, "u@test.pl", "password123").status_code == 401
    assert login(client, "u@test.pl", "newpass456").status_code == 200


def test_change_password_wrong_old(client, make_user):
    make_user("u@test.pl")
    login(client, "u@test.pl")
    r = client.post("/auth/change-password", json={"old_password": "WRONG-pass", "new_password": "newpass456"})
    assert r.status_code == 400
    assert r.json()["code"] == "WRONG_PASSWORD"
    assert login(client, "u@test.pl", "password123").status_code == 200


def test_change_password_same_and_too_short(client, make_user):
    make_user("u@test.pl")
    login(client, "u@test.pl")
    r = client.post("/auth/change-password", json={"old_password": "password123", "new_password": "password123"})
    assert r.status_code == 400
    assert r.json()["code"] == "SAME_PASSWORD"
    r = client.post("/auth/change-password", json={"old_password": "password123", "new_password": "short"})
    assert r.status_code == 422


def test_change_password_requires_auth(client):
    r = client.post("/auth/change-password", json={"old_password": "password123", "new_password": "newpass456"})
    assert r.status_code == 401


# ---------- регистрация -> карта пациента в recp_serv ----------

def test_register_creates_patient_card(client, recp):
    r = client.post("/auth/register", json=REG)
    assert recp.created_for == [r.json()["id"]]


def test_duplicate_register_does_not_call_recp(client, recp):
    client.post("/auth/register", json=REG)
    client.post("/auth/register", json=REG)
    assert len(recp.created_for) == 1


def test_recp_client_sends_contract_body(monkeypatch, make_user):
    user = make_user("p@test.pl")
    sent = {}

    def fake_post(url, json, headers, timeout):
        sent.update(url=url, json=json, headers=headers)
        return httpx.Response(200, request=httpx.Request("POST", url))

    monkeypatch.setattr(httpx, "post", fake_post)
    assert RecpClient("http://recp:8000/", "secret").create_patient_card(user) is True
    assert sent["url"] == "http://recp:8000/internal/patients"
    assert sent["headers"] == {"X-Internal-Token": "secret"}
    assert sent["json"]["user_id"] == sent["json"]["userId"] == user.id
    assert sent["json"]["email"] == "p@test.pl"


def test_register_survives_recp_down(client, monkeypatch):
    """recp_serv лежит — аккаунт всё равно создаётся."""
    from app.main import app
    from app.recp_client import get_recp_client

    def boom(*args, **kwargs):
        raise httpx.ConnectError("refused")

    monkeypatch.setattr(httpx, "post", boom)
    app.dependency_overrides[get_recp_client] = lambda: RecpClient("http://nowhere", "x")
    r = client.post("/auth/register", json=REG)
    assert r.status_code == 201

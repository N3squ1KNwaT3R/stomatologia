REG = {"email": "Jan@Test.pl", "password": "password123", "first_name": "Jan", "last_name": "K"}


def test_register_creates_patient(client):
    r = client.post("/auth/register", json=REG)
    assert r.status_code == 201
    body = r.json()
    assert body["role"] == "PATIENT"
    assert body["email"] == "jan@test.pl"
    assert "password_hash" not in body


def test_register_duplicate_email_case_insensitive(client):
    client.post("/auth/register", json=REG)
    r = client.post("/auth/register", json={**REG, "email": "jan@test.pl"})
    assert r.status_code == 409
    assert r.json()["code"] == "EMAIL_TAKEN"


def test_register_validation(client):
    r = client.post("/auth/register", json={**REG, "password": "short"})
    assert r.status_code == 422


def test_login_sets_cookie_and_me_works(client):
    client.post("/auth/register", json=REG)
    r = client.post("/auth/login", json={"email": "jan@test.pl", "password": "password123"})
    assert r.status_code == 200
    assert "access_token" in r.cookies
    me = client.get("/auth/me")  # TestClient хранит cookie
    assert me.status_code == 200
    assert me.json()["email"] == "jan@test.pl"


def test_bearer_token_works(client):
    client.post("/auth/register", json=REG)
    token = client.post("/auth/login", json={"email": "jan@test.pl", "password": "password123"}).json()["access_token"]
    client.cookies.clear()
    r = client.get("/auth/me", headers={"Authorization": f"Bearer {token}"})
    assert r.status_code == 200


def test_wrong_password_and_unknown_email_same_answer(client):
    client.post("/auth/register", json=REG)
    a = client.post("/auth/login", json={"email": "jan@test.pl", "password": "wrong-pass"})
    b = client.post("/auth/login", json={"email": "nobody@test.pl", "password": "wrong-pass"})
    assert a.status_code == b.status_code == 401
    assert a.json() == b.json()


def test_me_without_token(client):
    assert client.get("/auth/me").status_code == 401


def test_garbage_token(client):
    r = client.get("/auth/me", headers={"Authorization": "Bearer not-a-jwt"})
    assert r.status_code == 401


def test_logout_clears_cookie(client):
    client.post("/auth/register", json=REG)
    client.post("/auth/login", json={"email": "jan@test.pl", "password": "password123"})
    client.post("/auth/logout")
    assert client.get("/auth/me").status_code == 401


def test_deactivated_user_rejected(client, make_user, db):
    user = make_user()
    client.post("/auth/login", json={"email": "u@test.pl", "password": "password123"})
    user.is_active = False
    db.commit()
    assert client.get("/auth/me").status_code == 401


def test_require_role(client, make_user):
    make_user("p@test.pl", role="PATIENT")
    make_user("a@test.pl", role="ADMIN")
    client.post("/auth/login", json={"email": "p@test.pl", "password": "password123"})
    assert client.get("/auth/users").status_code == 403
    client.post("/auth/login", json={"email": "a@test.pl", "password": "password123"})
    assert client.get("/auth/users").status_code == 200

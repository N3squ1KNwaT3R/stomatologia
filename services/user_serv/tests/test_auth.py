from datetime import timedelta


def auth(t):
    return {"Authorization": f"Bearer {t}"}


def test_whoami(client, token):
    r = client.get("/whoami", headers=auth(token(42, "DOCTOR")))
    assert r.json() == {"id": 42, "role": "DOCTOR"}


def test_cookie_token(client, token):
    client.cookies.set("access_token", token(7))
    assert client.get("/whoami").json()["id"] == 7


def test_no_token(client):
    r = client.get("/whoami")
    assert r.status_code == 401
    assert r.json()["code"] == "NOT_AUTHENTICATED"


def test_expired_token(client, token):
    assert client.get("/whoami", headers=auth(token(ttl=timedelta(seconds=-1)))).status_code == 401


def test_foreign_signature(client, token):
    bad = token(secret="some-other-secret-that-is-also-long-enough")
    assert client.get("/whoami", headers=auth(bad)).status_code == 401


def test_require_role(client, token):
    assert client.get("/_test/doctor-only", headers=auth(token(role="PATIENT"))).status_code == 403
    assert client.get("/_test/doctor-only", headers=auth(token(role="DOCTOR"))).status_code == 200


def test_internal(client):
    assert client.get("/_test/internal").status_code == 403
    assert client.get("/_test/internal", headers={"X-Internal-Token": "nope"}).status_code == 403
    assert client.get("/_test/internal", headers={"X-Internal-Token": "dev-internal"}).status_code == 200

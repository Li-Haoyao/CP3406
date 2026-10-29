import hashlib
import sqlite3
from fastapi.testclient import TestClient
import pytest
from app import create_app

PASSWORD = "Collab-Radar-2026!"
NEW_PASSWORD = "A-New-Radar-Password!"

@pytest.fixture
def client(tmp_path):
    database = tmp_path / "accounts.db"
    with TestClient(create_app(database)) as api:
        api.database = database
        yield api

def register(api, email="first@example.test", name="Explorer"):
    response = api.post("/auth/register", json={"email": email, "password": PASSWORD, "displayName": name})
    assert response.status_code == 201, response.text
    return response.json()

def headers(session):
    return {"Authorization": "Bearer " + session["token"]}

def login(api, email="first@example.test", password=PASSWORD):
    return api.post("/auth/login", json={"email": email, "password": password})

def save(api, session, id="demo-tea", saved=True):
    return api.post("/me/favorites", headers=headers(session), json={"id": id, "saved": saved})

def test_registration_normalizes_identity_and_stores_only_hashes(client):
    session = register(client, " FIRST@Example.Test ", " Explorer ")
    assert session["user"]["email"] == "first@example.test"
    assert session["user"]["displayName"] == "Explorer"
    assert login(client).status_code == 200
    with sqlite3.connect(client.database) as db:
        assert db.execute("SELECT password_hash FROM users").fetchone()[0].startswith("$argon2id$")
        assert db.execute("SELECT token_hash FROM sessions WHERE token_hash=?", (hashlib.sha256(session["token"].encode()).hexdigest(),)).fetchone()
        assert not db.execute("SELECT token_hash FROM sessions WHERE token_hash=?", (session["token"],)).fetchone()

def test_duplicate_email_and_credentials(client):
    register(client)
    duplicate = client.post("/auth/register", json={"email": "FIRST@example.test", "password": PASSWORD, "displayName": "Second"})
    assert duplicate.status_code == 409
    for address in ("first@example.test", "unknown@example.test"):
        response = login(client, address, "wrong-password")
        assert response.status_code == 401
        assert response.json()["detail"] == "INVALID_CREDENTIALS"

def test_invalid_inputs_never_echo_passwords(client):
    body = {"email": "first@example.test", "password": "short", "displayName": "First"}
    assert client.post("/auth/register", json=body).json()["detail"] == "WEAK_PASSWORD"
    body.update(password=PASSWORD, email="bad-email")
    assert client.post("/auth/register", json=body).json()["detail"] == "INVALID_EMAIL"
    body.update(email="valid@example.test", displayName="a" * 41)
    response = client.post("/auth/register", json=body)
    assert response.status_code == 422
    assert PASSWORD not in response.text
    assert response.json() == {"detail": "INVALID_INPUT"}

def test_private_routes_require_valid_session(client):
    assert client.get("/me").status_code == 401
    assert client.post("/me/favorites", json={"id": "demo", "saved": True}).status_code == 401
    assert client.get("/me", headers={"Authorization": "Bearer invented"}).status_code == 401

def test_account_favorites_are_isolated_and_idempotent(client):
    first = register(client)
    second = register(client, "second@example.test")
    event_id = "event/with unicode 联动"
    for _ in range(2):
        assert save(client, first, event_id).json() == {"favorites": [event_id]}
    assert save(client, second, event_id, False).json() == {"favorites": []}
    assert client.get("/me", headers=headers(first)).json()["favorites"] == [event_id]
    assert save(client, first, event_id, False).json() == {"favorites": []}

def test_multiple_devices_share_profile_and_saves(client):
    first = register(client)
    second = login(client).json()
    assert first["token"] != second["token"]
    save(client, first)
    changed = client.patch("/me", headers=headers(first), json={"displayName": "探险者 தமிழ்"})
    assert changed.status_code == 200
    snapshot = client.get("/me", headers=headers(second)).json()
    assert snapshot["favorites"] == ["demo-tea"]
    assert snapshot["user"]["displayName"] == "探险者 தமிழ்"

def test_logout_revokes_only_current_session(client):
    first = register(client)
    second = login(client).json()
    assert client.post("/auth/logout", headers=headers(first)).status_code == 204
    assert client.get("/me", headers=headers(first)).status_code == 401
    assert client.get("/me", headers=headers(second)).status_code == 200

def test_password_change_requires_password_and_rotates_all_sessions(client):
    first = register(client)
    second = login(client).json()
    body = {"currentPassword": "wrong", "newPassword": NEW_PASSWORD}
    assert client.post("/auth/password", headers=headers(first), json=body).status_code == 401
    assert client.get("/me", headers=headers(first)).status_code == 200
    body["currentPassword"] = PASSWORD
    response = client.post("/auth/password", headers=headers(first), json=body)
    assert response.status_code == 200
    new = response.json()
    for old in (first, second):
        assert client.get("/me", headers=headers(old)).status_code == 401
        assert save(client, old).status_code == 401
    assert client.get("/me", headers=headers(new)).status_code == 200
    assert login(client).status_code == 401
    assert login(client, password=NEW_PASSWORD).status_code == 200

def test_delete_requires_password_cascades_and_preserves_other_accounts(client):
    first = register(client)
    second = register(client, "second@example.test")
    save(client, first)
    assert client.post("/me/delete", headers=headers(first), json={"password": "wrong"}).status_code == 401
    assert client.post("/me/delete", headers=headers(first), json={"password": PASSWORD}).status_code == 204
    assert client.get("/me", headers=headers(first)).status_code == 401
    assert client.get("/me", headers=headers(second)).status_code == 200
    with sqlite3.connect(client.database) as db:
        for table, column in (("users", "id"), ("sessions", "user_id"), ("favorites", "user_id")):
            assert db.execute(f"SELECT COUNT(*) FROM {table} WHERE {column}=?", (first["user"]["id"],)).fetchone()[0] == 0

def test_expired_session_is_rejected(client):
    session = register(client)
    with sqlite3.connect(client.database) as db:
        db.execute("UPDATE sessions SET expires_at=0")
    assert client.get("/me", headers=headers(session)).status_code == 401

def test_rate_limit_persists_failed_attempts(client, monkeypatch):
    monkeypatch.setattr("app.time.time", lambda: 1900000020)
    for _ in range(10):
        assert login(client, "unknown@example.test").status_code == 401
    assert login(client, "unknown@example.test").status_code == 429

def test_restart_preserves_session_and_favorites(tmp_path):
    database = tmp_path / "persistent.db"
    with TestClient(create_app(database)) as first_client:
        session = register(first_client)
        save(first_client, session)
    with TestClient(create_app(database)) as second_client:
        response = second_client.get("/me", headers=headers(session))
        assert response.status_code == 200
        assert response.json()["favorites"] == ["demo-tea"]

def test_favorites_limit_does_not_block_removal(client):
    session = register(client)
    with sqlite3.connect(client.database) as db:
        db.executemany("INSERT INTO favorites VALUES (?, ?)", [(session["user"]["id"], str(i)) for i in range(500)])
    assert save(client, session, "new").json()["detail"] == "FAVORITES_LIMIT"
    assert save(client, session, "0", False).status_code == 200
    assert save(client, session, "new").status_code == 200

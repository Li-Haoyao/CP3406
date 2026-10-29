"""Collab Radar account service. Run with: python -m uvicorn app:app --host 127.0.0.1 --port 8000."""
from contextlib import asynccontextmanager, contextmanager
from pathlib import Path
import hashlib
import os
import re
import secrets
import sqlite3
import time
import uuid
from fastapi import Depends, FastAPI, HTTPException, Request, Response
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field
from pwdlib import PasswordHash

class Login(BaseModel):
    email: str = Field(min_length=3, max_length=254)
    password: str = Field(min_length=1, max_length=128)

class Register(Login):
    displayName: str = Field(min_length=1, max_length=40)

class Rename(BaseModel):
    displayName: str = Field(min_length=1, max_length=40)

class PasswordChange(BaseModel):
    currentPassword: str = Field(min_length=1, max_length=128)
    newPassword: str = Field(min_length=12, max_length=128)

class DeleteAccount(BaseModel):
    password: str = Field(min_length=1, max_length=128)

class Favorite(BaseModel):
    id: str = Field(min_length=1, max_length=256)
    saved: bool

SESSION_SECONDS = 7 * 24 * 60 * 60

def create_app(database: str | Path | None = None) -> FastAPI:
    database = Path(database or os.environ.get("RADAR_DATABASE", Path(__file__).parent / "data" / "radar.db"))
    hasher = PasswordHash.recommended()
    dummy_hash = hasher.hash(secrets.token_urlsafe(32))
    bearer = HTTPBearer(auto_error=False)

    @contextmanager
    def db():
        connection = sqlite3.connect(database, timeout=15)
        connection.row_factory = sqlite3.Row
        connection.execute("PRAGMA foreign_keys=ON")
        try:
            with connection:
                yield connection
        finally:
            connection.close()

    @asynccontextmanager
    async def lifespan(_app):
        database.parent.mkdir(parents=True, exist_ok=True)
        with db() as connection:
            connection.execute("PRAGMA journal_mode=WAL")
            connection.executescript("""
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY, email TEXT NOT NULL UNIQUE,
                    display_name TEXT NOT NULL, password_hash TEXT NOT NULL);
                CREATE TABLE IF NOT EXISTS sessions (
                    token_hash TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    expires_at INTEGER NOT NULL);
                CREATE INDEX IF NOT EXISTS session_user ON sessions(user_id);
                CREATE TABLE IF NOT EXISTS favorites (
                    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, event_id TEXT NOT NULL,
                    PRIMARY KEY(user_id, event_id));
                CREATE TABLE IF NOT EXISTS attempts (
                    key_hash TEXT NOT NULL, bucket INTEGER NOT NULL, count INTEGER NOT NULL,
                    PRIMARY KEY(key_hash, bucket));
            """)
        yield

    app = FastAPI(title="Collab Radar Accounts", version="0.3.0", lifespan=lifespan)

    @app.exception_handler(RequestValidationError)
    async def invalid_body(_request, _error):
        # Default validation output may echo passwords. Return only a stable error code.
        return JSONResponse(status_code=422, content={"detail": "INVALID_INPUT"})

    @app.middleware("http")
    async def no_cache(request: Request, call_next):
        response = await call_next(request)
        response.headers["Cache-Control"] = "no-store"
        return response

    def fail(code, status=400):
        raise HTTPException(status_code=status, detail=code)

    def email(value):
        normalized = value.strip().lower()
        if not re.fullmatch(r"[^\s@]+@[^\s@]+\.[^\s@]+", normalized):
            fail("INVALID_EMAIL")
        return normalized

    def display_name(value):
        name = value.strip()
        if not 1 <= len(name) <= 40 or any(ord(c) < 32 for c in value):
            fail("INVALID_NAME")
        return name

    def password(value):
        if not 12 <= len(value) <= 128:
            fail("WEAK_PASSWORD")
        return value

    def digest(value):
        return hashlib.sha256(value.encode()).hexdigest()

    def throttle(key, limit, seconds=60):
        bucket = int(time.time()) // seconds * seconds
        with db() as connection:
            connection.execute("BEGIN IMMEDIATE")
            connection.execute("DELETE FROM attempts WHERE bucket < ?", (bucket - 86400,))
            connection.execute("INSERT INTO attempts VALUES (?, ?, 1) ON CONFLICT(key_hash,bucket) DO UPDATE SET count=count+1", (digest(key), bucket))
            count = connection.execute("SELECT count FROM attempts WHERE key_hash=? AND bucket=?", (digest(key), bucket)).fetchone()[0]
        if count > limit:
            fail("RATE_LIMITED", 429)

    def auth_throttle(request, address):
        throttle("ip:" + (request.client.host if request.client else "unknown"), 30)
        throttle("email:" + address, 10)

    def identity(credentials: HTTPAuthorizationCredentials | None = Depends(bearer)):
        if not credentials or credentials.scheme.lower() != "bearer":
            fail("SESSION_EXPIRED", 401)
        return digest(credentials.credentials)

    def user_for(connection, token_hash):
        user = connection.execute("SELECT users.* FROM users JOIN sessions ON users.id=sessions.user_id WHERE token_hash=? AND expires_at>?", (token_hash, int(time.time()))).fetchone()
        if not user:
            fail("SESSION_EXPIRED", 401)
        return user

    def profile(user):
        return {"id": user["id"], "email": user["email"], "displayName": user["display_name"]}

    def favorites(connection, user_id):
        return [row[0] for row in connection.execute("SELECT event_id FROM favorites WHERE user_id=? ORDER BY event_id", (user_id,))]

    def issue_session(connection, user):
        token = secrets.token_urlsafe(32)
        connection.execute("DELETE FROM sessions WHERE expires_at<=?", (int(time.time()),))
        connection.execute("INSERT INTO sessions VALUES (?, ?, ?)", (digest(token), user["id"], int(time.time()) + SESSION_SECONDS))
        return {"token": token, "user": profile(user), "favorites": favorites(connection, user["id"])}

    @app.get("/health")
    def health():
        return {"status": "ok", "version": "0.3.0"}

    @app.post("/auth/register", status_code=201)
    def register(body: Register, request: Request):
        address = email(body.email)
        auth_throttle(request, address)
        name = display_name(body.displayName)
        encoded = hasher.hash(password(body.password))
        with db() as connection:
            connection.execute("BEGIN IMMEDIATE")
            user_id = uuid.uuid4().hex
            try:
                connection.execute("INSERT INTO users VALUES (?, ?, ?, ?)", (user_id, address, name, encoded))
            except sqlite3.IntegrityError:
                fail("EMAIL_TAKEN", 409)
            return issue_session(connection, connection.execute("SELECT * FROM users WHERE id=?", (user_id,)).fetchone())

    @app.post("/auth/login")
    def login(body: Login, request: Request):
        address = email(body.email)
        auth_throttle(request, address)
        with db() as connection:
            connection.execute("BEGIN IMMEDIATE")
            user = connection.execute("SELECT * FROM users WHERE email=?", (address,)).fetchone()
            valid = hasher.verify(body.password, user["password_hash"] if user else dummy_hash)
            if not user or not valid:
                fail("INVALID_CREDENTIALS", 401)
            return issue_session(connection, user)

    @app.get("/me")
    def me(token_hash: str = Depends(identity)):
        with db() as connection:
            user = user_for(connection, token_hash)
            return {"user": profile(user), "favorites": favorites(connection, user["id"])}

    @app.patch("/me")
    def rename(body: Rename, token_hash: str = Depends(identity)):
        name = display_name(body.displayName)
        with db() as connection:
            connection.execute("BEGIN IMMEDIATE")
            user = user_for(connection, token_hash)
            connection.execute("UPDATE users SET display_name=? WHERE id=?", (name, user["id"]))
            return {**profile(user), "displayName": name}

    @app.post("/me/favorites")
    def save_favorite(body: Favorite, token_hash: str = Depends(identity)):
        if not body.id.strip() or any(ord(c) < 32 for c in body.id):
            fail("INVALID_INPUT")
        throttle("favorites:" + token_hash, 120)
        with db() as connection:
            connection.execute("BEGIN IMMEDIATE")
            user = user_for(connection, token_hash)
            ids = favorites(connection, user["id"])
            if body.saved:
                if body.id not in ids and len(ids) >= 500:
                    fail("FAVORITES_LIMIT")
                connection.execute("INSERT OR IGNORE INTO favorites VALUES (?, ?)", (user["id"], body.id))
            else:
                connection.execute("DELETE FROM favorites WHERE user_id=? AND event_id=?", (user["id"], body.id))
            return {"favorites": favorites(connection, user["id"])}

    @app.post("/auth/logout", status_code=204)
    def logout(token_hash: str = Depends(identity)):
        with db() as connection:
            connection.execute("DELETE FROM sessions WHERE token_hash=?", (token_hash,))
        return Response(status_code=204)

    @app.post("/auth/password")
    def change_password(body: PasswordChange, token_hash: str = Depends(identity)):
        throttle("password:" + token_hash, 5)
        with db() as connection:
            connection.execute("BEGIN IMMEDIATE")
            user = user_for(connection, token_hash)
            if not hasher.verify(body.currentPassword, user["password_hash"]):
                fail("INVALID_CREDENTIALS", 401)
            connection.execute("UPDATE users SET password_hash=? WHERE id=?", (hasher.hash(password(body.newPassword)), user["id"]))
            connection.execute("DELETE FROM sessions WHERE user_id=?", (user["id"],))
            return issue_session(connection, user)

    @app.post("/me/delete", status_code=204)
    def delete_account(body: DeleteAccount, token_hash: str = Depends(identity)):
        throttle("delete:" + token_hash, 5)
        with db() as connection:
            connection.execute("BEGIN IMMEDIATE")
            user = user_for(connection, token_hash)
            if not hasher.verify(body.password, user["password_hash"]):
                fail("INVALID_CREDENTIALS", 401)
            connection.execute("DELETE FROM users WHERE id=?", (user["id"],))
        return Response(status_code=204)

    return app

app = create_app()

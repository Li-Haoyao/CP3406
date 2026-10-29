# Collab Radar Accounts · v0.3

An initial, runnable account service built with Python 3.12, FastAPI, SQLite, and Argon2id. No public instance has been deployed.

## Run locally

On Windows, run `run_server.bat`. On other systems, run the following commands from the `server/` directory:

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python -m uvicorn app:app --host 127.0.0.1 --port 8000 --workers 1
```

The default database is `server/data/radar.db`. Tables are created automatically on first startup. Accounts, favourites, and unexpired sessions persist across restarts. Set `RADAR_DATABASE` to an absolute path to use a different database location. Do not commit the actual database to the source repository.

## API endpoints

| Method | Path | Request body | Response |
| --- | --- | --- | --- |
| GET | `/health` | — | Status and version |
| POST | `/auth/register` | email, password, displayName | token, user, favorites |
| POST | `/auth/login` | email, password | token, user, favorites |
| GET | `/me` | — | user, favorites |
| PATCH | `/me` | displayName | user |
| POST | `/me/favorites` | id, saved (boolean) | favorites |
| POST | `/auth/password` | currentPassword, newPassword | New token, user, favorites |
| POST | `/auth/logout` | — | 204 |
| POST | `/me/delete` | password | 204 |

All routes except registration, login, and the health check use `Authorization: Bearer <token>`.

Favourite IDs are supplied in JSON fields and may contain slashes and Unicode characters. Each account can save up to 500 items. Write operations validate the session within a transaction to prevent revoked sessions from modifying data after a password change.

## Session and security behaviour

Random session tokens expire after seven days. The database stores only the SHA-256 hash of each token.

- Changing a password revokes all previous sessions.
- Signing out revokes only the current token.
- Deleting an account also deletes its favourites and sessions through cascading relationships.
- Validation errors return stable error codes without echoing passwords.
- Login and registration are rate-limited by source IP address and email. Other sensitive operations are also rate-limited. Counters are stored in the database and remain effective across restarts.

## Deployment

Deployment requires an HTTPS reverse proxy, persistent database storage, controlled file permissions, regular backups, and recovery verification.

The current implementation is intended for an initial single-instance deployment. Multiple instances require shared database and rate-limiting storage. Configure Uvicorn to trust only explicitly identified proxy sources; do not unconditionally trust arbitrary forwarded IP headers. Do not log Authorization headers or request passwords.

Email addresses currently serve only as login identifiers; ownership is not verified. Email verification, password-reset email, SMS, and third-party identity services are not implemented. No emails are sent by default. Complete these account-lifecycle features before opening registration to real users.

## Tests

Install the development dependencies, then run the tests from the `server/` directory:

```bash
python -m pip install -r requirements-dev.txt
python -m pytest -q
```

Tests use temporary databases and do not access actual user data.

`requirements-tested.txt` records the complete dependency versions used for validation, including test dependencies. To reproduce that environment, run:

```bash
pip install -r requirements-tested.txt
```

## Reference

[FastAPI password hashing documentation](https://fastapi.tiangolo.com/tutorial/security/oauth2-jwt/).

This project uses revocable, opaque session tokens rather than JWTs.

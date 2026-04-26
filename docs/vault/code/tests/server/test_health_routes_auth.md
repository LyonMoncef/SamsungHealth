---
type: code-source
language: python
file_path: tests/server/test_health_routes_auth.py
git_blob: 01d9d13dd41a803e978e02e2b112e01066a4cc2c
last_synced: '2026-04-26T16:48:28Z'
loc: 184
annotations: []
imports:
- pytest
exports:
- _register_and_login
- TestHealthRequiresAuth
- TestUserDataIsolation
tags:
- code
- python
---

# tests/server/test_health_routes_auth.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_health_routes_auth.py`](../../../tests/server/test_health_routes_auth.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3 — Health routers must require auth + enforce user_id isolation.

Tests RED-first contre les 5 routers santé (sleep/heartrate/steps/exercise/mood):
- TestHealthRequiresAuth : GET sans Authorization → 401 (currently 200, RED → GREEN après V2.3)
- TestUserDataIsolation : user A ne peut pas lire les données de user B (filtering user_id)

Spec: docs/vault/specs/2026-04-26-v2-auth-foundation.md (#29-#37)
"""
from __future__ import annotations

import pytest


_TEST_JWT_SECRET = "dGVzdC1qd3Qtc2VjcmV0LXdpdGgtMzItYnl0ZXMtbWluLW9rITE="
_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


@pytest.fixture(autouse=True)
def _set_auth_env(monkeypatch):
    monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", _TEST_JWT_SECRET)
    monkeypatch.setenv("SAMSUNGHEALTH_REGISTRATION_TOKEN", _TEST_REGISTRATION_TOKEN)


def _register_and_login(client, email="user-a@example.com", password="longpassword12345"):
    client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )
    r = client.post("/auth/login", json={"email": email, "password": password})
    return r.json()


class TestHealthRequiresAuth:
    def test_sleep_get_without_token_401(self, client_pg_ready):
        """given no Authorization header, when GET /api/sleep, then 401.

        spec #29.
        """
        client = client_pg_ready
        r = client.get("/api/sleep?from=2026-01-01&to=2026-12-31")
        assert r.status_code == 401, f"expected 401 unauth, got {r.status_code}: {r.text}"

    def test_heartrate_get_without_token_401(self, client_pg_ready):
        """given no Authorization, when GET /api/heartrate, then 401.

        spec #30.
        """
        client = client_pg_ready
        r = client.get("/api/heartrate?from=2026-01-01&to=2026-12-31")
        assert r.status_code == 401

    def test_steps_get_without_token_401(self, client_pg_ready):
        """given no Authorization, when GET /api/steps, then 401.

        spec #31.
        """
        client = client_pg_ready
        r = client.get("/api/steps?from=2026-01-01&to=2026-12-31")
        assert r.status_code == 401

    def test_exercise_get_without_token_401(self, client_pg_ready):
        """given no Authorization, when GET /api/exercise, then 401.

        spec #32.
        """
        client = client_pg_ready
        r = client.get("/api/exercise?from=2026-01-01&to=2026-12-31")
        assert r.status_code == 401

    def test_mood_get_without_token_401(self, client_pg_ready):
        """given no Authorization, when GET /api/mood, then 401.

        spec #33.
        """
        client = client_pg_ready
        r = client.get("/api/mood?from=2026-01-01&to=2026-12-31")
        assert r.status_code == 401

    def test_sleep_post_without_token_401(self, client_pg_ready):
        """given no Authorization, when POST /api/sleep, then 401.

        spec #34.
        """
        client = client_pg_ready
        payload = {
            "sessions": [
                {"sleep_start": "2026-04-20T23:00:00", "sleep_end": "2026-04-21T07:00:00", "stages": []}
            ]
        }
        r = client.post("/api/sleep", json=payload)
        assert r.status_code == 401


class TestUserDataIsolation:
    def test_user_a_cannot_read_user_b_sleep(self, client_pg_ready):
        """given user A POSTs sleep, when user B GETs sleep, then empty list (filtering by user_id).

        spec #35.
        """
        client = client_pg_ready
        a = _register_and_login(client, email="iso-a@example.com")
        b = _register_and_login(client, email="iso-b@example.com")

        a_headers = {"Authorization": f"Bearer {a['access_token']}"}
        b_headers = {"Authorization": f"Bearer {b['access_token']}"}

        payload = {
            "sessions": [
                {"sleep_start": "2026-04-20T23:00:00", "sleep_end": "2026-04-21T07:00:00", "stages": []}
            ]
        }
        post_a = client.post("/api/sleep", headers=a_headers, json=payload)
        assert post_a.status_code == 201, f"user A POST sleep should succeed: {post_a.text}"

        # User B reads → must NOT see user A's data.
        r = client.get(
            "/api/sleep?from=2026-04-20&to=2026-04-22",
            headers=b_headers,
        )
        assert r.status_code == 200, f"user B GET should be 200 (auth ok), got {r.status_code}: {r.text}"
        assert r.json() == [], f"user B should NOT see user A's data, got: {r.json()}"

    def test_user_a_cannot_read_user_b_mood(self, client_pg_ready):
        """given user A POSTs mood, when user B GETs mood, then empty list.

        spec #36.
        """
        client = client_pg_ready
        a = _register_and_login(client, email="iso-mood-a@example.com")
        b = _register_and_login(client, email="iso-mood-b@example.com")

        a_headers = {"Authorization": f"Bearer {a['access_token']}"}
        b_headers = {"Authorization": f"Bearer {b['access_token']}"}

        payload = {
            "moods": [
                {
                    "recorded_at": "2026-04-20T20:00:00",
                    "mood_score": 7,
                    "tags": [],
                }
            ]
        }
        post_a = client.post("/api/mood", headers=a_headers, json=payload)
        assert post_a.status_code in (200, 201), f"user A POST mood should succeed: {post_a.text}"

        r = client.get(
            "/api/mood?from=2026-04-20&to=2026-04-22",
            headers=b_headers,
        )
        assert r.status_code == 200
        assert r.json() == [], f"user B should NOT see user A's mood, got: {r.json()}"

    def test_user_a_post_associates_with_user_a_id(self, client_pg_ready, db_session):
        """given user A POSTs sleep, when querying DB directly, then row.user_id == user A's id.

        spec #37 — server injects user_id from token, not from body.
        """
        from sqlalchemy import select

        from server.db.models import SleepSession, User

        client = client_pg_ready
        a = _register_and_login(client, email="assoc-a@example.com")
        a_headers = {"Authorization": f"Bearer {a['access_token']}"}

        payload = {
            "sessions": [
                {"sleep_start": "2026-04-20T23:00:00", "sleep_end": "2026-04-21T07:00:00", "stages": []}
            ]
        }
        post = client.post("/api/sleep", headers=a_headers, json=payload)
        assert post.status_code == 201, f"POST sleep failed: {post.text}"

        user_a = db_session.execute(
            select(User).where(User.email == "assoc-a@example.com")
        ).scalar_one()
        sessions = db_session.execute(select(SleepSession)).scalars().all()
        assert len(sessions) >= 1
        for s in sessions:
            assert s.user_id == user_a.id, (
                f"sleep session user_id should be user A ({user_a.id}), got {s.user_id}"
            )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register_and_login` (function) — lines 24-31
- `TestHealthRequiresAuth` (class) — lines 34-92
- `TestUserDataIsolation` (class) — lines 95-184

### Imports
- `pytest`

### Exports
- `_register_and_login`
- `TestHealthRequiresAuth`
- `TestUserDataIsolation`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2-auth-foundation]] — classes: `TestHealthRequiresAuth`, `TestUserDataIsolation` · methods: `test_sleep_get_without_token_401`, `test_heartrate_get_without_token_401`, `test_steps_get_without_token_401`, `test_exercise_get_without_token_401`, `test_mood_get_without_token_401`, `test_sleep_post_without_token_401`, `test_user_a_cannot_read_user_b_sleep`, `test_user_a_cannot_read_user_b_mood`, `test_user_a_post_associates_with_user_a_id`

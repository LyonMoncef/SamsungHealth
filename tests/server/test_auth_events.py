"""V2.3 — Audit trail (auth_events) DB writes.

Tests RED-first contre `server.db.models.AuthEvent`:
chaque op auth (register/login_success/login_failure/refresh) doit écrire 1 row
avec event_type approprié + email_hash (jamais email plain).

Spec: docs/vault/specs/2026-04-26-v2-auth-foundation.md (#45-#48)
"""
from __future__ import annotations

import hashlib

import pytest


_TEST_JWT_SECRET = "dGVzdC1qd3Qtc2VjcmV0LXdpdGgtMzItYnl0ZXMtbWluLW9rITE="
_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


@pytest.fixture(autouse=True)
def _set_auth_env(monkeypatch):
    monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", _TEST_JWT_SECRET)
    monkeypatch.setenv("SAMSUNGHEALTH_REGISTRATION_TOKEN", _TEST_REGISTRATION_TOKEN)


def _register(client, email="ev@example.com", password="longpassword12345"):
    return client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )


class TestAuthEventLogging:
    def test_login_success_writes_event(self, client_pg_ready, db_session):
        """given a registered user, when POST /auth/login succeeds, then 1 row in auth_events with event_type='login_success' + user_id + email_hash.

        spec #45.
        """
        from sqlalchemy import select

        from server.db.models import AuthEvent, User

        client = client_pg_ready
        _register(client, email="success-evt@example.com")
        r = client.post(
            "/auth/login",
            json={"email": "success-evt@example.com", "password": "longpassword12345"},
        )
        assert r.status_code == 200, f"login should succeed: {r.text}"

        user = db_session.execute(
            select(User).where(User.email == "success-evt@example.com")
        ).scalar_one()

        events = db_session.execute(
            select(AuthEvent).where(AuthEvent.event_type == "login_success")
        ).scalars().all()
        assert len(events) >= 1, "expected at least 1 login_success auth_event row"
        evt = events[-1]
        assert evt.user_id == user.id
        expected_hash = hashlib.sha256(b"success-evt@example.com").hexdigest()
        assert evt.email_hash == expected_hash, (
            f"email_hash should be sha256(lowercased email), got {evt.email_hash}"
        )

    def test_login_failure_writes_event_with_email_hash_not_plain(self, client_pg_ready, db_session):
        """given a failed login (unknown user), when row is inserted in auth_events, then email_hash present and NO column contains email plain.

        spec #46 — never store email plain in audit row (RGPD).
        """
        from sqlalchemy import select

        from server.db.models import AuthEvent

        client = client_pg_ready
        # Unknown user — login fails.
        r = client.post(
            "/auth/login",
            json={"email": "unknown-evt@example.com", "password": "anypassword12345"},
        )
        assert r.status_code == 401

        events = db_session.execute(
            select(AuthEvent).where(AuthEvent.event_type == "login_failure")
        ).scalars().all()
        assert len(events) >= 1, "expected at least 1 login_failure auth_event row"
        evt = events[-1]

        expected_hash = hashlib.sha256(b"unknown-evt@example.com").hexdigest()
        assert evt.email_hash == expected_hash, (
            f"email_hash mismatch: got {evt.email_hash}, expected {expected_hash}"
        )

        # No column should contain the email plain.
        for col_name in evt.__table__.columns.keys():
            value = getattr(evt, col_name)
            if isinstance(value, str):
                assert "unknown-evt@example.com" not in value, (
                    f"column {col_name} leaks email plain: {value}"
                )

    def test_register_writes_event(self, client_pg_ready, db_session):
        """given POST /auth/register success, when row is inserted in auth_events, then event_type='register'.

        spec #47.
        """
        from sqlalchemy import select

        from server.db.models import AuthEvent

        client = client_pg_ready
        r = _register(client, email="reg-evt@example.com")
        assert r.status_code == 201

        events = db_session.execute(
            select(AuthEvent).where(AuthEvent.event_type == "register")
        ).scalars().all()
        assert len(events) >= 1, "expected at least 1 register auth_event row"

    def test_refresh_writes_event(self, client_pg_ready, db_session):
        """given POST /auth/refresh success, when row is inserted in auth_events, then event_type='refresh'.

        spec #48.
        """
        from sqlalchemy import select

        from server.db.models import AuthEvent

        client = client_pg_ready
        _register(client, email="refresh-evt@example.com")
        login = client.post(
            "/auth/login",
            json={"email": "refresh-evt@example.com", "password": "longpassword12345"},
        )
        refresh_token = login.json()["refresh_token"]

        r = client.post("/auth/refresh", json={"refresh_token": refresh_token})
        assert r.status_code == 200, f"refresh should succeed: {r.text}"

        events = db_session.execute(
            select(AuthEvent).where(AuthEvent.event_type == "refresh")
        ).scalars().all()
        assert len(events) >= 1, "expected at least 1 refresh auth_event row"

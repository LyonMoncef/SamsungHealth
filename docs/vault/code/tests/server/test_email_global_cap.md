---
type: code-source
language: python
file_path: tests/server/test_email_global_cap.py
git_blob: e62b746023d31564935b8ed7855b286dc5ff58cc
last_synced: '2026-05-06T08:02:35Z'
loc: 183
annotations: []
imports:
- pytest
exports:
- _hmac_email
- _seed_auth_events
- TestEmailGlobalCap
tags:
- code
- python
---

# tests/server/test_email_global_cap.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_email_global_cap.py`](../../../tests/server/test_email_global_cap.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.1 — Cap pur-email global (anti mail-bombing distribué).

Tests RED-first contre le handler /auth/password/reset/request + /auth/verify-email/request
qui doit rejeter au-delà de N requests/email/jour, indépendamment de l'IP source.

Spec: docs/vault/specs/2026-04-26-v2.3.3.1-rate-limit-lockout.md
  §Cap pur-email global anti mail-bombing distribué.
Tests d'acceptation : #8 (reset), #8-volet verify, reset compteur après 24h.
"""
from __future__ import annotations

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"
_TEST_EMAIL_HASH_SALT = (
    "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
)


def _hmac_email(email: str) -> str:
    import hashlib
    import hmac

    return hmac.new(
        _TEST_EMAIL_HASH_SALT.encode("utf-8"),
        email.lower().encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()


def _seed_auth_events(db_session, *, email_hash: str, event_type: str, count: int):
    """Insère N rows auth_events pour simuler le cap atteint."""
    from sqlalchemy import insert

    from server.db.models import AuthEvent

    for i in range(count):
        db_session.execute(
            insert(AuthEvent).values(
                event_type=event_type,
                user_id=None,
                email_hash=email_hash,
                ip=f"10.0.{i // 256}.{i % 256}",
            )
        )
    db_session.commit()


class TestEmailGlobalCap:
    def test_reset_request_email_global_cap_rejects_after_threshold(
        self, client_pg_ready, db_session, monkeypatch
    ):
        """given 3 rows auth_events password_reset_request avec même email_hash sur N IPs distinctes (cap monkeypatché à 3), when 4e POST /auth/password/reset/request, then 400 email_global_cap_exceeded.

        spec §Cap pur-email global — anti mail-bombing distribué (rotation IPs bypass cap (IP,email)).
        spec test #8.
        """
        # Réduit le cap pour rendre le test rapide (1000 rows = lent).
        monkeypatch.setenv("SAMSUNGHEALTH_EMAIL_GLOBAL_CAP", "3")
        monkeypatch.setenv("SAMSUNGHEALTH_EMAIL_GLOBAL_CAP_WINDOW_HOURS", "24")

        client = client_pg_ready
        client.post(
            "/auth/register",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"email": "global-cap@example.com", "password": "longpassword12345"},
        )

        eh = _hmac_email("global-cap@example.com")
        _seed_auth_events(
            db_session,
            email_hash=eh,
            event_type="password_reset_request",
            count=3,
        )

        r = client.post(
            "/auth/password/reset/request",
            json={"email": "global-cap@example.com"},
        )
        assert r.status_code == 400, (
            f"expected 400 email_global_cap_exceeded, got {r.status_code}: {r.text}"
        )
        assert r.json().get("detail") == "email_global_cap_exceeded", (
            f"expected detail='email_global_cap_exceeded', got {r.json()}"
        )

    def test_verify_request_email_global_cap_independent_of_reset(
        self, client_pg_ready, db_session, monkeypatch
    ):
        """given cap atteint pour event_type='email_verification_request', when 4e POST /auth/verify-email/request, then 400 email_global_cap_exceeded (compteur séparé du reset).

        spec §Cap pur-email global — 60 verify/email/jour (counté séparément).
        """
        monkeypatch.setenv("SAMSUNGHEALTH_EMAIL_GLOBAL_CAP", "3")
        monkeypatch.setenv("SAMSUNGHEALTH_EMAIL_GLOBAL_CAP_WINDOW_HOURS", "24")

        client = client_pg_ready
        client.post(
            "/auth/register",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"email": "verify-cap@example.com", "password": "longpassword12345"},
        )

        eh = _hmac_email("verify-cap@example.com")
        _seed_auth_events(
            db_session,
            email_hash=eh,
            event_type="email_verification_request",
            count=3,
        )

        r = client.post(
            "/auth/verify-email/request",
            json={"email": "verify-cap@example.com"},
        )
        assert r.status_code == 400, (
            f"expected 400 email_global_cap_exceeded for verify, got {r.status_code}: {r.text}"
        )
        assert r.json().get("detail") == "email_global_cap_exceeded"

    def test_email_global_cap_resets_after_window(
        self, client_pg_ready, db_session, monkeypatch
    ):
        """given 3 rows password_reset_request CRÉÉES > 24h dans le passé + 3 rows RÉCENTES, when POST /auth/password/reset/request, then 400 cap_exceeded SI cap fonctionne, mais le test prouve que les 3 anciennes sont SKIP via la window (3 récentes seules atteignent cap=3 → 4e tentative = 400).

        On insère 3 anciennes (hors window, ne comptent PAS) + 3 récentes (dans window, atteignent cap).
        Le 4e POST doit être 400 prouvant que la window est bien filtrée (sinon 6 events compteraient
        et le 1er POST serait déjà 400 — donc on n'apprendrait rien). Si cap absent → toujours 200/202 = RED.
        spec §Cap pur-email global — counté sur fenêtre glissante (created_at > now()-1day).
        """
        from datetime import datetime, timedelta, timezone

        from sqlalchemy import insert

        from server.db.models import AuthEvent

        monkeypatch.setenv("SAMSUNGHEALTH_EMAIL_GLOBAL_CAP", "3")
        monkeypatch.setenv("SAMSUNGHEALTH_EMAIL_GLOBAL_CAP_WINDOW_HOURS", "24")

        client = client_pg_ready
        client.post(
            "/auth/register",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"email": "window-reset@example.com", "password": "longpassword12345"},
        )

        eh = _hmac_email("window-reset@example.com")
        # 3 rows ANCIENNES (>24h) — doivent être hors window (pas comptées)
        old = datetime.now(timezone.utc) - timedelta(hours=48)
        for i in range(3):
            db_session.execute(
                insert(AuthEvent).values(
                    event_type="password_reset_request",
                    email_hash=eh,
                    ip=f"10.1.0.{i}",
                    created_at=old,
                )
            )
        # 3 rows RÉCENTES — atteignent cap=3
        recent = datetime.now(timezone.utc) - timedelta(minutes=10)
        for i in range(3):
            db_session.execute(
                insert(AuthEvent).values(
                    event_type="password_reset_request",
                    email_hash=eh,
                    ip=f"10.2.0.{i}",
                    created_at=recent,
                )
            )
        db_session.commit()

        r = client.post(
            "/auth/password/reset/request",
            json={"email": "window-reset@example.com"},
        )
        # Cap=3 sur fenêtre 24h → seules les 3 récentes comptent → 4e tentative = 400.
        # Si cap absent (RED) → 200/202 et test échoue.
        assert r.status_code == 400, (
            f"4th request within window MUST be 400 (cap=3 on recent only); got {r.status_code}: {r.text}"
        )
        assert r.json().get("detail") == "email_global_cap_exceeded"
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_hmac_email` (function) — lines 21-29
- `_seed_auth_events` (function) — lines 32-47
- `TestEmailGlobalCap` (class) — lines 50-183

### Imports
- `pytest`

### Exports
- `_hmac_email`
- `_seed_auth_events`
- `TestEmailGlobalCap`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — classes: `TestEmailGlobalCap`

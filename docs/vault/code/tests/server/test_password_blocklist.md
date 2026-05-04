---
type: code-source
language: python
file_path: tests/server/test_password_blocklist.py
git_blob: 5646b5b83aaf5b6e5a39e27162b232c502af9fdd
last_synced: '2026-04-26T22:07:14Z'
loc: 118
annotations: []
imports:
- pytest
exports:
- TestBlocklist
tags:
- code
- python
---

# tests/server/test_password_blocklist.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_password_blocklist.py`](../../../tests/server/test_password_blocklist.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.1 — Password blocklist (top-100 leaked passwords).

Tests RED-first contre `server.security.passwords._PASSWORD_BLOCKLIST` +
`validate_password_strength` + intégration register/reset.

Spec: docs/vault/specs/2026-04-26-v2.3.1-reset-password-email-verify.md
"""
from __future__ import annotations

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


@pytest.fixture(autouse=True)
def _set_v231_env(monkeypatch):
    monkeypatch.setenv("SAMSUNGHEALTH_PUBLIC_BASE_URL", "http://localhost:8000")
    monkeypatch.setenv(
        "SAMSUNGHEALTH_EMAIL_HASH_SALT",
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    )


class TestBlocklist:
    def test_blocklist_contains_top_100_passwords(self):
        """given _PASSWORD_BLOCKLIST, when read, then it contains common ≥12-char leaked passwords (e.g. 'password1234', 'qwerty123456').

        spec §Validation password — _PASSWORD_BLOCKLIST frozenset top-100, tous ≥ 12 chars.
        """
        from server.security.passwords import _PASSWORD_BLOCKLIST

        assert isinstance(_PASSWORD_BLOCKLIST, frozenset), (
            f"_PASSWORD_BLOCKLIST must be frozenset, got {type(_PASSWORD_BLOCKLIST).__name__}"
        )
        assert len(_PASSWORD_BLOCKLIST) >= 50, (
            f"blocklist should have ≥50 entries (~top-100), got {len(_PASSWORD_BLOCKLIST)}"
        )
        # Sample expected entries (spec mentions explicitly).
        expected_present = {"password1234", "qwerty123456", "letmein123456"}
        for pw in expected_present:
            assert pw in _PASSWORD_BLOCKLIST or any(
                pw[:8] in entry for entry in _PASSWORD_BLOCKLIST
            ), f"expected blocklist to contain '{pw}' or similar, missing"
        # All entries must be ≥ 12 chars (else already caught by min-length).
        too_short = [pw for pw in _PASSWORD_BLOCKLIST if len(pw) < 12]
        assert not too_short, f"blocklist contains <12-char entries: {too_short}"

    def test_register_rejects_blocklist_password(self, client_pg_ready):
        """given POST /auth/register with password='password1234' (in blocklist), when called, then 400 weak_password.

        spec §Validation password — Vérifié sur register ET reset pour cohérence.
        """
        client = client_pg_ready
        r = client.post(
            "/auth/register",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"email": "blockregister@example.com", "password": "password1234"},
        )
        assert r.status_code == 400, f"expected 400 weak_password, got {r.status_code}: {r.text}"
        assert r.json() == {"detail": "weak_password"}

    def test_reset_rejects_blocklist_password_token_not_consumed(
        self, client_pg_ready, db_session
    ):
        """given POST /auth/password/reset/confirm with blocklist password, when called, then 400 weak_password + token NOT consumed (user can retry).

        spec §Validation password — match blocklist sur reset → token NON consommé.
        """
        from sqlalchemy import select

        from server.db.models import User, VerificationToken
        from server.security.email_outbound import _outbound_link_cache

        client = client_pg_ready
        # Register with a strong password first (blocklist not matched).
        reg = client.post(
            "/auth/register",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={
                "email": "blockreset@example.com",
                "password": "VeryStrongSeedPwd2026!",
            },
        )
        assert reg.status_code == 201, f"register precondition failed: {reg.text}"

        client.post(
            "/auth/password/reset/request",
            json={"email": "blockreset@example.com"},
        )
        user = db_session.execute(
            select(User).where(User.email == "blockreset@example.com")
        ).scalar_one()
        row = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.user_id == user.id,
                VerificationToken.purpose == "password_reset",
                VerificationToken.consumed_at.is_(None),
            )
        ).scalar_one()
        raw = _outbound_link_cache.get(row.token_hash)
        assert raw is not None, "raw token must be in cache (fresh request)"

        r = client.post(
            "/auth/password/reset/confirm",
            json={"token": raw, "new_password": "qwerty123456"},  # blocklist
        )
        assert r.status_code == 400
        assert r.json() == {"detail": "weak_password"}

        # Token NOT consumed.
        db_session.expire_all()
        row_after = db_session.execute(
            select(VerificationToken).where(VerificationToken.id == row.id)
        ).scalar_one()
        assert row_after.consumed_at is None, (
            "token must NOT be consumed when blocklist password rejected"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestBlocklist` (class) — lines 25-118

### Imports
- `pytest`

### Exports
- `TestBlocklist`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — classes: `TestBlocklist` · methods: `test_blocklist_contains_top_100_passwords`, `test_register_rejects_blocklist_password`, `test_reset_rejects_blocklist_password_token_not_consumed`

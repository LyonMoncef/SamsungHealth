---
type: code-source
language: python
file_path: tests/server/test_alembic_0006.py
git_blob: 7d7f85d28dd1240c94b4d09802ebb6e3fae1a2ee
last_synced: '2026-05-06T08:02:34Z'
loc: 235
annotations: []
imports:
- os
- subprocess
- datetime
- pytest
exports:
- _run_alembic
- TestUpgradeDowngrade
tags:
- code
- python
---

# tests/server/test_alembic_0006.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_alembic_0006.py`](../../../tests/server/test_alembic_0006.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.1 — Alembic migration 0006_verification_tokens.

Tests RED-first contre `alembic/versions/0006_verification_tokens.py`.

Spec: docs/vault/specs/2026-04-26-v2.3.1-reset-password-email-verify.md
"""
from __future__ import annotations

import os
import subprocess
from datetime import datetime, timedelta, timezone

import pytest


def _run_alembic(cmd: list[str], pg_url: str) -> subprocess.CompletedProcess:
    env = os.environ.copy()
    env["DATABASE_URL"] = pg_url
    return subprocess.run(
        ["alembic"] + cmd,
        capture_output=True,
        text=True,
        env=env,
        check=False,
    )


class TestUpgradeDowngrade:
    def test_upgrade_creates_verification_tokens_table_with_constraints(self, pg_url, engine):
        """given alembic upgrade head, when DB inspected, then verification_tokens exists with all expected columns + check constraint consumed_after_issued.

        spec §Schéma verification_tokens — table + check constraint consumed_after_issued.
        spec §Test d'acceptation #23 — bonnes colonnes/index/FK + check constraint.
        """
        from sqlalchemy import inspect, text

        result = _run_alembic(["upgrade", "head"], pg_url)
        assert result.returncode == 0, f"upgrade failed: {result.stderr}"

        inspector = inspect(engine)
        assert "verification_tokens" in inspector.get_table_names(), (
            "verification_tokens table not created"
        )
        cols = {c["name"] for c in inspector.get_columns("verification_tokens")}
        expected = {
            "id",
            "user_id",
            "token_hash",
            "purpose",
            "issued_at",
            "expires_at",
            "consumed_at",
            "ip",
            "user_agent",
        }
        missing = expected - cols
        assert not missing, f"missing columns: {missing}"

        # Check constraint: try insert with consumed_at < issued_at → must fail.
        with engine.begin() as conn:
            conn.execute(
                text(
                    "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
                    "VALUES (gen_random_uuid(), 'check-cons@x.com', 'x', true, now())"
                )
            )
        with engine.begin() as conn:
            user_id = conn.execute(
                text("SELECT id FROM users WHERE email = 'check-cons@x.com'")
            ).scalar_one()
            now = datetime.now(timezone.utc)
            with pytest.raises(Exception):
                conn.execute(
                    text(
                        "INSERT INTO verification_tokens "
                        "(id, user_id, token_hash, purpose, issued_at, expires_at, consumed_at) "
                        "VALUES (gen_random_uuid(), :uid, 'cc-test-hash-1', 'email_verification', "
                        ":issued, :expires, :consumed)"
                    ),
                    {
                        "uid": user_id,
                        "issued": now,
                        "expires": now + timedelta(hours=1),
                        "consumed": now - timedelta(seconds=10),  # BEFORE issued → CHECK fail
                    },
                )

    def test_upgrade_creates_unique_partial_index_active_per_purpose(self, pg_url, engine):
        """given alembic upgrade head, when 2 active tokens inserted for same (user, purpose), then 2nd raises (unique partial index WHERE consumed_at IS NULL).

        spec §Schéma — UNIQUE INDEX uq_verification_tokens_active_per_purpose.
        """
        from sqlalchemy import text

        result = _run_alembic(["upgrade", "head"], pg_url)
        assert result.returncode == 0, f"upgrade failed: {result.stderr}"

        with engine.begin() as conn:
            conn.execute(
                text(
                    "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
                    "VALUES (gen_random_uuid(), 'unique-idx@x.com', 'x', true, now())"
                )
            )
            user_id = conn.execute(
                text("SELECT id FROM users WHERE email = 'unique-idx@x.com'")
            ).scalar_one()
            now = datetime.now(timezone.utc)
            conn.execute(
                text(
                    "INSERT INTO verification_tokens "
                    "(id, user_id, token_hash, purpose, issued_at, expires_at) "
                    "VALUES (gen_random_uuid(), :uid, 'hash-active-1', 'password_reset', :issued, :exp)"
                ),
                {"uid": user_id, "issued": now, "exp": now + timedelta(hours=1)},
            )

        with pytest.raises(Exception):
            with engine.begin() as conn2:
                conn2.execute(
                    text(
                        "INSERT INTO verification_tokens "
                        "(id, user_id, token_hash, purpose, issued_at, expires_at) "
                        "VALUES (gen_random_uuid(), :uid, 'hash-active-2', 'password_reset', :issued, :exp)"
                    ),
                    {
                        "uid": user_id,
                        "issued": now,
                        "exp": now + timedelta(hours=1),
                    },
                )

    def test_upgrade_creates_anti_flip_back_trigger(self, pg_url, engine):
        """given a consumed token row, when UPDATE attempts to NULL consumed_at, then trigger raises.

        spec §Schéma — CREATE TRIGGER trg_verification_tokens_no_unconsume.
        """
        from sqlalchemy import text

        result = _run_alembic(["upgrade", "head"], pg_url)
        assert result.returncode == 0, f"upgrade failed: {result.stderr}"

        with engine.begin() as conn:
            conn.execute(
                text(
                    "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
                    "VALUES (gen_random_uuid(), 'flipback-mig@x.com', 'x', true, now())"
                )
            )
            user_id = conn.execute(
                text("SELECT id FROM users WHERE email = 'flipback-mig@x.com'")
            ).scalar_one()
            now = datetime.now(timezone.utc)
            conn.execute(
                text(
                    "INSERT INTO verification_tokens "
                    "(id, user_id, token_hash, purpose, issued_at, expires_at, consumed_at) "
                    "VALUES (gen_random_uuid(), :uid, 'hash-flip-mig-1', 'email_verification', :issued, :exp, :cons)"
                ),
                {
                    "uid": user_id,
                    "issued": now,
                    "exp": now + timedelta(hours=24),
                    "cons": now,
                },
            )

        with pytest.raises(Exception):
            with engine.begin() as conn2:
                conn2.execute(
                    text(
                        "UPDATE verification_tokens SET consumed_at = NULL "
                        "WHERE token_hash = 'hash-flip-mig-1'"
                    )
                )

    def test_downgrade_drops_table_function_trigger_clean(self, pg_url, engine):
        """given alembic upgrade to revision 0006 (9d2e3f5a6b71) then downgrade -1, when inspected, then verification_tokens table + function + trigger are removed.

        spec §Livrables — Downgrade DROP TABLE + DROP FUNCTION.
        Updated for V2.3.2 (migration 0007 added on top): pin upgrade target
        to revision 0006 instead of head so adding migrations downstream
        does not break this test.
        """
        from sqlalchemy import inspect, text

        # Pin to revision 0006 regardless of current state (downgrade if downstream).
        _run_alembic(["downgrade", "base"], pg_url)
        up = _run_alembic(["upgrade", "9d2e3f5a6b71"], pg_url)
        assert up.returncode == 0, f"upgrade failed: {up.stderr}"

        # PRE-CONDITION: 0006 applied (table exists + revision is 0006).
        inspector = inspect(engine)
        assert "verification_tokens" in inspector.get_table_names(), (
            "PRE-CONDITION fail: verification_tokens must exist after upgrade 0006 "
            "(migration 0006 missing)"
        )
        with engine.connect() as conn:
            head_rev = conn.execute(text("SELECT version_num FROM alembic_version")).scalar()
            assert head_rev == "9d2e3f5a6b71", (
                f"PRE-CONDITION fail: expected head=9d2e3f5a6b71 (0006), got {head_rev!r}"
            )
            # Function must exist before downgrade.
            fn_before = conn.execute(
                text(
                    "SELECT proname FROM pg_proc WHERE proname = 'verification_tokens_no_unconsume'"
                )
            ).fetchone()
            assert fn_before is not None, (
                "PRE-CONDITION fail: verification_tokens_no_unconsume function must exist after upgrade"
            )

        # Downgrade by 1 step (back to 0005).
        down = _run_alembic(["downgrade", "-1"], pg_url)
        assert down.returncode == 0, f"downgrade -1 failed: {down.stderr}"

        inspector = inspect(engine)
        assert "verification_tokens" not in inspector.get_table_names(), (
            "verification_tokens must be dropped on downgrade"
        )

        # Function must be gone too.
        with engine.connect() as conn:
            row = conn.execute(
                text(
                    "SELECT proname FROM pg_proc WHERE proname = 'verification_tokens_no_unconsume'"
                )
            ).fetchone()
            assert row is None, "verification_tokens_no_unconsume function must be dropped"

        # Other tables (users, refresh_tokens, auth_events) must still exist.
        remaining = set(inspector.get_table_names())
        assert "users" in remaining, "downgrade must not drop users table"
        assert "refresh_tokens" in remaining, "downgrade must not drop refresh_tokens"
        assert "auth_events" in remaining, "downgrade must not drop auth_events"
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_run_alembic` (function) — lines 16-25
- `TestUpgradeDowngrade` (class) — lines 28-235

### Imports
- `os`
- `subprocess`
- `datetime`
- `pytest`

### Exports
- `_run_alembic`
- `TestUpgradeDowngrade`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — classes: `TestUpgradeDowngrade` · methods: `test_upgrade_creates_verification_tokens_table_with_constraints`, `test_upgrade_creates_unique_partial_index_active_per_purpose`, `test_upgrade_creates_anti_flip_back_trigger`, `test_downgrade_drops_table_function_trigger_clean`

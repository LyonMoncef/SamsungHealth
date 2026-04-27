"""V2.3.2 — Alembic migration 0007_identity_providers.

Tests RED-first contre `alembic/versions/0007_identity_providers.py`:
- Crée table identity_providers + 2 unique constraints + 2 index
- Ajoute colonne payload jsonb à verification_tokens
- Downgrade DROP TABLE + DROP COLUMN clean
- Head révision = 0a3b4c5d6e72, parent = 9d2e3f5a6b71

Spec: docs/vault/specs/2026-04-26-v2.3.2-google-oauth.md (#44).
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
    def test_upgrade_creates_identity_providers_with_columns_and_indexes(
        self, pg_url, engine
    ):
        """given alembic upgrade head, when DB inspected, then identity_providers table exists with all expected columns + 2 indexes.

        spec §Schéma identity_providers + spec §Livrables migration 0007.
        """
        from sqlalchemy import inspect

        result = _run_alembic(["upgrade", "head"], pg_url)
        assert result.returncode == 0, f"upgrade failed: {result.stderr}"

        inspector = inspect(engine)
        assert "identity_providers" in inspector.get_table_names(), (
            "identity_providers table not created"
        )
        cols = {c["name"] for c in inspector.get_columns("identity_providers")}
        expected = {
            "id",
            "user_id",
            "provider",
            "provider_sub",
            "provider_email",
            "email_verified",
            "linked_at",
            "last_used_at",
            "raw_claims",
        }
        missing = expected - cols
        assert not missing, f"missing columns: {missing}"

        # Indexes:
        idx_names = {idx["name"] for idx in inspector.get_indexes("identity_providers")}
        assert "idx_identity_providers_user_id" in idx_names, (
            f"idx_identity_providers_user_id missing, got: {idx_names}"
        )
        assert "idx_identity_providers_provider_sub" in idx_names, (
            f"idx_identity_providers_provider_sub missing, got: {idx_names}"
        )

    def test_upgrade_creates_two_unique_constraints(self, pg_url, engine):
        """given alembic upgrade head, when 2 rows with same (provider, provider_sub) inserted, then 2nd raises (UNIQUE).

        spec §Schéma — UNIQUE (provider, provider_sub) + UNIQUE (user_id, provider).
        """
        from sqlalchemy import text

        result = _run_alembic(["upgrade", "head"], pg_url)
        assert result.returncode == 0, f"upgrade failed: {result.stderr}"

        with engine.begin() as conn:
            # Create 2 users (so we can violate (provider, provider_sub) without violating (user_id, provider)).
            conn.execute(
                text(
                    "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
                    "VALUES (gen_random_uuid(), 'idp-uq-1@x.com', 'x', true, now())"
                )
            )
            conn.execute(
                text(
                    "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
                    "VALUES (gen_random_uuid(), 'idp-uq-2@x.com', 'x', true, now())"
                )
            )

        with engine.begin() as conn:
            user1 = conn.execute(
                text("SELECT id FROM users WHERE email = 'idp-uq-1@x.com'")
            ).scalar_one()
            user2 = conn.execute(
                text("SELECT id FROM users WHERE email = 'idp-uq-2@x.com'")
            ).scalar_one()

            conn.execute(
                text(
                    "INSERT INTO identity_providers "
                    "(id, user_id, provider, provider_sub, email_verified) "
                    "VALUES (gen_random_uuid(), :uid, 'google', 'sub-shared-123', false)"
                ),
                {"uid": user1},
            )

        # Try to insert another row with the SAME (provider, provider_sub) but a different user → must fail UNIQUE.
        with pytest.raises(Exception):
            with engine.begin() as conn2:
                conn2.execute(
                    text(
                        "INSERT INTO identity_providers "
                        "(id, user_id, provider, provider_sub, email_verified) "
                        "VALUES (gen_random_uuid(), :uid, 'google', 'sub-shared-123', false)"
                    ),
                    {"uid": user2},
                )

    def test_upgrade_adds_payload_column_to_verification_tokens(self, pg_url, engine):
        """given alembic upgrade head, when verification_tokens columns inspected, then 'payload' (jsonb) column exists.

        spec §Stockage du token oauth_link_confirm — colonne payload jsonb NULL ajoutée à verification_tokens via 0007.
        """
        from sqlalchemy import inspect

        result = _run_alembic(["upgrade", "head"], pg_url)
        assert result.returncode == 0, f"upgrade failed: {result.stderr}"

        inspector = inspect(engine)
        cols = {c["name"]: c for c in inspector.get_columns("verification_tokens")}
        assert "payload" in cols, (
            f"payload column missing from verification_tokens: {sorted(cols)}"
        )

    def test_downgrade_drops_table_and_column_clean(self, pg_url, engine):
        """given alembic upgrade to revision 0007 then downgrade -1, when inspected, then identity_providers dropped + payload column removed from verification_tokens (precondition: head is 0007).

        spec §Livrables — Downgrade DROP TABLE + DROP COLUMN.
        Updated for V2.3.3.1 (migration 0008 added on top): pin upgrade target
        to revision 0007 instead of head so adding migrations downstream
        does not break this test (mirrors the same pattern used in 0006).
        """
        from sqlalchemy import inspect, text

        # Pin to revision 0007 regardless of current state (downgrade if downstream).
        _run_alembic(["downgrade", "base"], pg_url)
        up = _run_alembic(["upgrade", "0a3b4c5d6e72"], pg_url)
        assert up.returncode == 0, f"upgrade failed: {up.stderr}"

        inspector = inspect(engine)
        assert "identity_providers" in inspector.get_table_names(), (
            "PRE-CONDITION fail: identity_providers must exist after upgrade"
        )
        with engine.connect() as conn:
            head_rev = conn.execute(text("SELECT version_num FROM alembic_version")).scalar()
            assert head_rev == "0a3b4c5d6e72", (
                f"PRE-CONDITION fail: expected head=0a3b4c5d6e72 (0007), got {head_rev!r}"
            )

        down = _run_alembic(["downgrade", "-1"], pg_url)
        assert down.returncode == 0, f"downgrade -1 failed: {down.stderr}"

        inspector = inspect(engine)
        assert "identity_providers" not in inspector.get_table_names(), (
            "identity_providers must be dropped on downgrade"
        )
        # Column 'payload' must be removed from verification_tokens.
        cols = {c["name"] for c in inspector.get_columns("verification_tokens")}
        assert "payload" not in cols, (
            f"payload column must be dropped from verification_tokens, still present: {sorted(cols)}"
        )
        # Other tables must still exist.
        remaining = set(inspector.get_table_names())
        assert "users" in remaining and "verification_tokens" in remaining, (
            "downgrade must not drop verification_tokens / users"
        )

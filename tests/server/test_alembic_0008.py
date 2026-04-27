"""V2.3.3.1 — Alembic migration 0008_users_last_failed_login.

Tests RED-first contre `alembic/versions/0008_users_last_failed_login.py`:
- Ajoute colonne users.last_failed_login_at TIMESTAMPTZ NULL
- Downgrade DROP COLUMN clean
- Head révision = 1b4c5d6e7f83, parent = 0a3b4c5d6e72

Spec: docs/vault/specs/2026-04-26-v2.3.3.1-rate-limit-lockout.md
  §Migration alembic 0008 (nouvelle colonne).
"""
from __future__ import annotations

import os
import subprocess

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
    def test_upgrade_adds_last_failed_login_at_column(self, pg_url, engine):
        """given alembic upgrade head, when DB inspected, then users.last_failed_login_at column exists (TIMESTAMPTZ nullable).

        spec §Migration alembic 0008 — ajout colonne pour cleanup_stale_failed_login_counts.
        """
        from sqlalchemy import inspect

        result = _run_alembic(["upgrade", "head"], pg_url)
        assert result.returncode == 0, f"upgrade failed: {result.stderr}"

        inspector = inspect(engine)
        cols = {c["name"]: c for c in inspector.get_columns("users")}
        assert "last_failed_login_at" in cols, (
            f"users.last_failed_login_at column missing: {sorted(cols)}"
        )
        col = cols["last_failed_login_at"]
        assert col.get("nullable") is True, (
            "last_failed_login_at MUST be nullable"
        )

    def test_upgrade_head_revision_matches_spec(self, pg_url, engine):
        """given alembic upgrade head, when alembic_version inspected, then revision == '1b4c5d6e7f83'.

        spec §Livrables — alembic 0008 revision id 1b4c5d6e7f83, parent 0a3b4c5d6e72.
        """
        from sqlalchemy import text

        result = _run_alembic(["upgrade", "head"], pg_url)
        assert result.returncode == 0, f"upgrade failed: {result.stderr}"

        with engine.connect() as conn:
            head_rev = conn.execute(text("SELECT version_num FROM alembic_version")).scalar()
        assert head_rev == "1b4c5d6e7f83", (
            f"expected alembic head=1b4c5d6e7f83 (0008), got {head_rev!r}"
        )

    def test_downgrade_drops_last_failed_login_at_clean(self, pg_url, engine):
        """given alembic upgrade head + downgrade -1, when inspected, then last_failed_login_at column dropped + previous tables intact.

        spec §Migration alembic 0008 — downgrade DROP COLUMN clean.
        """
        from sqlalchemy import inspect, text

        up = _run_alembic(["upgrade", "head"], pg_url)
        assert up.returncode == 0, f"upgrade failed: {up.stderr}"

        inspector = inspect(engine)
        cols = {c["name"] for c in inspector.get_columns("users")}
        assert "last_failed_login_at" in cols, (
            "PRE-CONDITION: last_failed_login_at must exist after upgrade"
        )
        with engine.connect() as conn:
            head_rev = conn.execute(text("SELECT version_num FROM alembic_version")).scalar()
            assert head_rev == "1b4c5d6e7f83", (
                f"PRE-CONDITION: head must be 1b4c5d6e7f83, got {head_rev!r}"
            )

        down = _run_alembic(["downgrade", "-1"], pg_url)
        assert down.returncode == 0, f"downgrade -1 failed: {down.stderr}"

        inspector = inspect(engine)
        cols_after = {c["name"] for c in inspector.get_columns("users")}
        assert "last_failed_login_at" not in cols_after, (
            f"last_failed_login_at MUST be dropped on downgrade, still present in {sorted(cols_after)}"
        )
        # Other essential columns intact
        for must_have in ("email", "password_hash", "failed_login_count", "locked_until"):
            assert must_have in cols_after, (
                f"downgrade MUST NOT drop existing column {must_have!r}, got {sorted(cols_after)}"
            )
        # Other tables intact
        tables = set(inspector.get_table_names())
        assert "users" in tables and "auth_events" in tables, (
            "downgrade MUST NOT drop sibling tables"
        )

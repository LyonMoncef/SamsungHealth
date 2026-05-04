"""
Tests RED — bootstrap schéma PG via Alembic (spec: V2.1 postgres-migration).

Mappé sur frontmatter tested_by: tests/server/test_postgres_bootstrap.py
Classes: TestBootstrap
Methods: test_alembic_upgrade_creates_schema, test_alembic_idempotent, test_alembic_downgrade_reverses

Ces tests exigent `alembic` + `sqlalchemy` installés et un Postgres dispo
(via fixture `pg_url` du conftest — testcontainers-postgres).
"""
import os
import subprocess

import pytest


EXPECTED_TABLES = {
    "sleep_sessions",
    "sleep_stages",
    "steps_hourly",
    "steps_daily",
    "heart_rate_hourly",
    "exercise_sessions",
    "stress",
    "spo2",
    "respiratory_rate",
    "hrv",
    "skin_temperature",
    "weight",
    "height",
    "blood_pressure",
    "mood",
    "water_intake",
    "activity_daily",
    "vitality_score",
    "floors_daily",
    "activity_level",
    "ecg",
    "alembic_version",  # table technique alembic
}


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


class TestBootstrap:
    def test_alembic_upgrade_creates_schema(self, pg_url, engine):
        # spec: "Bootstrap idempotent" — §Tests d'acceptation #1
        from sqlalchemy import inspect

        result = _run_alembic(["upgrade", "head"], pg_url)
        assert result.returncode == 0, f"alembic upgrade head a échoué : {result.stderr}"

        inspector = inspect(engine)
        present = set(inspector.get_table_names())
        missing = EXPECTED_TABLES - present
        assert not missing, f"Tables manquantes après upgrade : {missing}"

        # PK de sleep_sessions doit être UUID, pas integer
        pk_cols = inspector.get_pk_constraint("sleep_sessions")["constrained_columns"]
        assert pk_cols == ["id"], f"PK sleep_sessions = {pk_cols}, attendu ['id']"
        id_col = {c["name"]: c for c in inspector.get_columns("sleep_sessions")}["id"]
        assert "UUID" in str(id_col["type"]).upper(), (
            f"Type de sleep_sessions.id = {id_col['type']}, attendu UUID"
        )

        # created_at + updated_at obligatoires partout (sauf alembic_version)
        for table in EXPECTED_TABLES - {"alembic_version"}:
            cols = {c["name"] for c in inspector.get_columns(table)}
            assert "created_at" in cols, f"{table}.created_at manquant"
            assert "updated_at" in cols, f"{table}.updated_at manquant"

    def test_alembic_idempotent(self, pg_url):
        # spec: "Upgrade rejouable" — §Tests d'acceptation #2
        # 1ère fois : applique la migration
        first = _run_alembic(["upgrade", "head"], pg_url)
        assert first.returncode == 0, f"1er upgrade échoué : {first.stderr}"

        # 2e fois : doit être no-op, exit 0, aucune migration appliquée
        second = _run_alembic(["upgrade", "head"], pg_url)
        assert second.returncode == 0, f"2e upgrade échoué : {second.stderr}"
        # Alembic log "INFO  [alembic.runtime.migration] Running upgrade" n'apparait que si une migration est appliquée
        assert "Running upgrade" not in second.stdout + second.stderr, (
            "2e upgrade a réappliqué une migration — non idempotent"
        )

    def test_alembic_downgrade_reverses(self, pg_url, engine):
        # spec: "Downgrade réversible" — §Tests d'acceptation #3
        from sqlalchemy import inspect

        up = _run_alembic(["upgrade", "head"], pg_url)
        assert up.returncode == 0

        down = _run_alembic(["downgrade", "base"], pg_url)
        assert down.returncode == 0, f"downgrade base échoué : {down.stderr}"

        inspector = inspect(engine)
        remaining = set(inspector.get_table_names()) - {"alembic_version"}
        assert remaining == set(), f"Tables restantes après downgrade base : {remaining}"

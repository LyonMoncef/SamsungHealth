"""
Tests RED — V2.1.1 cutover routers + sentinelles SQLite cleanup.

Mappé sur frontmatter tested_by: tests/server/test_routers_cutover.py
Classes: TestHeartRateRouter, TestStepsRouter, TestExerciseRouter, TestNoSqliteResidual
"""
import re
import subprocess
from datetime import datetime, timezone
from pathlib import Path

import pytest


REPO_ROOT = Path(__file__).resolve().parents[2]


# schema_ready + client_pg_ready vivent dans tests/server/conftest.py


class TestHeartRateRouter:
    def test_post_get_heart_rate_round_trip(self, client_pg_ready, db_session):
        client_pg = client_pg_ready
        # spec V2.1.1 §Tests d'acceptation #2
        from sqlalchemy import select
        from server.db.models import HeartRateHourly

        payload = {"records": [
            {"date": "2026-04-20", "hour": 8, "min_bpm": 60, "max_bpm": 95, "avg_bpm": 72, "sample_count": 60},
            {"date": "2026-04-20", "hour": 9, "min_bpm": 65, "max_bpm": 110, "avg_bpm": 80, "sample_count": 60},
            {"date": "2026-04-21", "hour": 8, "min_bpm": 58, "max_bpm": 90, "avg_bpm": 70, "sample_count": 60},
        ]}
        r = client_pg.post("/api/heartrate", json=payload)
        assert r.status_code == 201, r.text
        assert r.json() == {"inserted": 3, "skipped": 0}

        # Vérification : les rows sont en PG (preuve que le router utilise SessionLocal, pas SQLite)
        pg_rows = db_session.execute(select(HeartRateHourly)).scalars().all()
        assert len(pg_rows) == 3, f"Attendu 3 rows en PG, got {len(pg_rows)} (router utilise encore SQLite ?)"

        r = client_pg.get("/api/heartrate", params={"from": "2026-04-20", "to": "2026-04-20"})
        assert r.status_code == 200
        rows = r.json()
        assert len(rows) == 2
        assert all(row["date"] == "2026-04-20" for row in rows)
        assert rows[0]["hour"] == 8 and rows[1]["hour"] == 9


class TestStepsRouter:
    def test_post_get_steps_round_trip(self, client_pg_ready, db_session):
        client_pg = client_pg_ready
        # spec V2.1.1 §Tests d'acceptation #3
        from sqlalchemy import select
        from server.db.models import StepsHourly

        payload = {"records": [
            {"date": "2026-04-20", "hour": 12, "step_count": 1500},
            {"date": "2026-04-20", "hour": 13, "step_count": 200},
            {"date": "2026-04-21", "hour": 9, "step_count": 850},
        ]}
        r = client_pg.post("/api/steps", json=payload)
        assert r.status_code == 201, r.text
        assert r.json() == {"inserted": 3, "skipped": 0}

        pg_rows = db_session.execute(select(StepsHourly)).scalars().all()
        assert len(pg_rows) == 3, f"Attendu 3 rows en PG, got {len(pg_rows)} (router utilise encore SQLite ?)"

        r = client_pg.get("/api/steps", params={"from": "2026-04-20", "to": "2026-04-21"})
        assert r.status_code == 200
        rows = r.json()
        assert len(rows) == 3
        total = sum(row["step_count"] for row in rows)
        assert total == 2550


class TestExerciseRouter:
    def test_post_get_exercise_round_trip(self, client_pg_ready, db_session):
        client_pg = client_pg_ready
        # spec V2.1.1 §Tests d'acceptation #4
        from sqlalchemy import select
        from server.db.models import ExerciseSession

        payload = {"sessions": [
            {
                "exercise_type": "running",
                "exercise_start": "2026-04-20T07:00:00+00:00",
                "exercise_end": "2026-04-20T07:45:00+00:00",
                "duration_minutes": 45.0,
            },
            {
                "exercise_type": "cycling",
                "exercise_start": "2026-04-21T08:00:00+00:00",
                "exercise_end": "2026-04-21T09:00:00+00:00",
                "duration_minutes": 60.0,
            },
        ]}
        r = client_pg.post("/api/exercise", json=payload)
        assert r.status_code == 201, r.text
        assert r.json() == {"inserted": 2, "skipped": 0}

        pg_rows = db_session.execute(select(ExerciseSession)).scalars().all()
        assert len(pg_rows) == 2, f"Attendu 2 rows en PG, got {len(pg_rows)} (router utilise encore SQLite ?)"

        r = client_pg.get("/api/exercise", params={"from": "2026-04-20", "to": "2026-04-21"})
        assert r.status_code == 200
        rows = r.json()
        assert len(rows) == 2
        types = {row["exercise_type"] for row in rows}
        assert types == {"running", "cycling"}


class TestNoSqliteResidual:
    def test_no_sqlite_imports_in_server(self):
        # spec V2.1.1 §Tests d'acceptation #5
        offenders: list[str] = []
        pattern = re.compile(r"^\s*(import sqlite3|from sqlite3 )", re.MULTILINE)
        for py in (REPO_ROOT / "server").rglob("*.py"):
            if pattern.search(py.read_text(encoding="utf-8")):
                offenders.append(str(py.relative_to(REPO_ROOT)))
        assert offenders == [], f"Fichiers server/ avec import sqlite3 résiduel : {offenders}"

    def test_no_health_db_in_repo(self):
        # spec V2.1.1 §Tests d'acceptation #6
        assert not (REPO_ROOT / "health.db").exists(), (
            "health.db doit avoir été supprimé du repo (V2.1.1 cutover)"
        )

    def test_get_connection_removed(self):
        # spec V2.1.1 §Tests d'acceptation #7
        with pytest.raises(ImportError):
            from server.database import get_connection  # noqa: F401

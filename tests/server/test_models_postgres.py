"""
Tests RED — persistence models + back-compat API (spec: V2.1 postgres-migration).

Mappé sur frontmatter tested_by: tests/server/test_models_postgres.py
Classes: TestSleepSessionPersistence, TestApiBackCompat
Methods:
  - test_insert_sleep_session_assigns_uuid_and_created_at
  - test_read_sleep_session_by_uuid
  - test_sleep_session_with_stages_atomic
  - test_get_sleep_period_6m_response_shape_unchanged
"""
import os
import subprocess
import uuid
from datetime import datetime, timezone

import pytest


def _alembic_upgrade(pg_url: str):
    env = os.environ.copy()
    env["DATABASE_URL"] = pg_url
    result = subprocess.run(
        ["alembic", "upgrade", "head"],
        capture_output=True, text=True, env=env, check=False,
    )
    assert result.returncode == 0, f"alembic upgrade head a échoué : {result.stderr}"


@pytest.fixture
def schema_ready(pg_url):
    _alembic_upgrade(pg_url)
    yield pg_url


class TestSleepSessionPersistence:
    def test_insert_sleep_session_assigns_uuid_and_created_at(self, schema_ready, db_session):
        # spec: "Insert sleep session avec UUID + timestamps auto" — §Tests d'acceptation #7
        from server.db.models import SleepSession

        session = SleepSession(
            sleep_start=datetime(2026, 4, 24, 22, 30, tzinfo=timezone.utc),
            sleep_end=datetime(2026, 4, 25, 6, 45, tzinfo=timezone.utc),
            sleep_duration_min=495,
        )
        db_session.add(session)
        db_session.commit()
        db_session.refresh(session)

        assert isinstance(session.id, uuid.UUID), f"id doit être UUID, got {type(session.id)}"
        assert session.id.version == 7, f"id.version = {session.id.version}, attendu 7"
        assert session.created_at is not None
        assert session.updated_at is not None
        assert session.created_at == session.updated_at, (
            "À l'insert, created_at et updated_at doivent être égaux"
        )

    def test_read_sleep_session_by_uuid(self, schema_ready, db_session):
        # spec: "Read by UUID" — §Tests d'acceptation #8
        from server.db.models import SleepSession

        s1 = SleepSession(
            sleep_start=datetime(2026, 4, 20, 22, 0, tzinfo=timezone.utc),
            sleep_end=datetime(2026, 4, 21, 6, 0, tzinfo=timezone.utc),
        )
        db_session.add(s1)
        db_session.commit()
        target_uuid = s1.id

        db_session.expire_all()
        found = db_session.get(SleepSession, target_uuid)
        assert found is not None
        assert found.id == target_uuid
        assert found.sleep_start == datetime(2026, 4, 20, 22, 0, tzinfo=timezone.utc)

    def test_sleep_session_with_stages_atomic(self, schema_ready, db_session):
        # spec: "Atomicité sleep_session + sleep_stages" — §Tests d'acceptation #9
        from sqlalchemy.exc import IntegrityError

        from server.db.models import SleepSession, SleepStage

        session = SleepSession(
            sleep_start=datetime(2026, 4, 22, 22, 0, tzinfo=timezone.utc),
            sleep_end=datetime(2026, 4, 23, 6, 0, tzinfo=timezone.utc),
        )
        # stage avec session_id invalide volontairement pour forcer un rollback
        bad_stage = SleepStage(
            session_id=uuid.uuid4(),  # UUID aléatoire qui n'existe pas → viole FK
            stage_type="DEEP",
            stage_start=datetime(2026, 4, 22, 22, 30, tzinfo=timezone.utc),
            stage_end=datetime(2026, 4, 22, 23, 0, tzinfo=timezone.utc),
        )
        db_session.add(session)
        db_session.add(bad_stage)
        with pytest.raises(IntegrityError):
            db_session.commit()
        db_session.rollback()

        # Après rollback, la session ne doit PAS être persistée
        from sqlalchemy import select
        result = db_session.execute(select(SleepSession)).scalars().all()
        assert result == [], f"Rollback manqué, SleepSession présents : {result}"


class TestApiBackCompat:
    def test_get_sleep_period_6m_response_shape_unchanged(self, schema_ready, db_session):
        # spec: "Back-compat API frontend" — §Tests d'acceptation #10
        # Le frontend Nightfall attend un shape JSON stable sur GET /api/sleep?period=6m
        # Seed minimal + appel endpoint + assertions sur keys/types (pas sur IDs UUID exacts)
        from datetime import date, timedelta

        from fastapi.testclient import TestClient

        from server.db.models import SleepSession
        from server.main import app

        # Seed 2 sessions il y a 1 mois
        base = datetime.combine(date.today() - timedelta(days=30), datetime.min.time(), tzinfo=timezone.utc)
        for i in range(2):
            db_session.add(
                SleepSession(
                    sleep_start=base.replace(hour=22) + timedelta(days=i),
                    sleep_end=base.replace(hour=6) + timedelta(days=i + 1),
                    sleep_duration_min=480,
                    sleep_score=80 + i,
                )
            )
        db_session.commit()

        with TestClient(app) as client:
            resp = client.get("/api/sleep", params={"period": "6m"})

        assert resp.status_code == 200, f"Status {resp.status_code} : {resp.text}"
        payload = resp.json()
        assert isinstance(payload, list), f"Payload doit être une liste, got {type(payload)}"
        assert len(payload) >= 2, f"Au moins 2 sessions seedées, got {len(payload)}"

        # Shape d'une session (clés que Nightfall consomme)
        first = payload[0]
        expected_keys = {"id", "sleep_start", "sleep_end", "sleep_duration_min", "sleep_score"}
        missing = expected_keys - set(first.keys())
        assert not missing, f"Clés manquantes dans la réponse : {missing} (payload : {first})"

        # Types
        assert isinstance(first["id"], str), "id doit être sérialisé en str (UUID)"
        assert isinstance(first["sleep_start"], str), "sleep_start doit être str ISO"
        assert isinstance(first["sleep_end"], str), "sleep_end doit être str ISO"
        assert isinstance(first["sleep_duration_min"], int)

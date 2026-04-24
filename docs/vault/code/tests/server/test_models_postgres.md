---
type: code-source
language: python
file_path: tests/server/test_models_postgres.py
git_blob: dd707e7520ad5665d648bfe09c02362bc21e815f
last_synced: '2026-04-24T02:26:12Z'
loc: 148
annotations: []
imports:
- os
- subprocess
- uuid
- datetime
- pytest
exports:
- TestSleepSessionPersistence
- TestApiBackCompat
tags:
- code
- python
---

# tests/server/test_models_postgres.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_models_postgres.py`](../../../tests/server/test_models_postgres.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
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


# schema_ready vit dans tests/server/conftest.py


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
    def test_get_sleep_response_shape_unchanged(self, schema_ready, client_pg, db_session):
        # spec V2.1.1 — §Tests d'acceptation #1 : back-compat shape JSON sur params réels Nightfall
        # Le frontend appelle GET /api/sleep?from=YYYY-MM-DD&to=YYYY-MM-DD&include_stages=true
        # (params adaptés depuis l'ancien `period=6m` qui n'existait pas dans le frontend réel)
        from datetime import date, timedelta

        from server.db.models import SleepSession, SleepStage

        d_start = date.today() - timedelta(days=2)
        base = datetime.combine(d_start, datetime.min.time(), tzinfo=timezone.utc)
        s1 = SleepSession(
            sleep_start=base.replace(hour=22),
            sleep_end=base.replace(hour=6) + timedelta(days=1),
        )
        db_session.add(s1)
        db_session.flush()
        db_session.add(
            SleepStage(
                session_id=s1.id,
                stage_type="deep",
                stage_start=base.replace(hour=23),
                stage_end=base.replace(hour=23) + timedelta(hours=1),
            )
        )
        db_session.commit()

        resp = client_pg.get(
            "/api/sleep",
            params={
                "from": d_start.isoformat(),
                "to": (d_start + timedelta(days=1)).isoformat(),
                "include_stages": "true",
            },
        )

        assert resp.status_code == 200, f"Status {resp.status_code} : {resp.text}"
        payload = resp.json()
        assert isinstance(payload, list)
        assert len(payload) >= 1

        first = payload[0]
        expected_keys = {"id", "sleep_start", "sleep_end", "stages"}
        missing = expected_keys - set(first.keys())
        assert not missing, f"Clés manquantes : {missing} (payload : {first})"

        # id passe en str (UUID sérialisé) — break-change attendu, frontend Nightfall ne consomme l'id qu'en passe-plat
        assert isinstance(first["id"], str)
        assert isinstance(first["sleep_start"], str)
        assert isinstance(first["sleep_end"], str)

        # Stages présents et bien formatés
        assert isinstance(first["stages"], list)
        assert len(first["stages"]) >= 1
        stage = first["stages"][0]
        for k in ("stage_type", "stage_start", "stage_end"):
            assert k in stage, f"Clé {k} manquante dans stage"
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestSleepSessionPersistence` (class) — lines 23-89
- `TestApiBackCompat` (class) — lines 92-148

### Imports
- `os`
- `subprocess`
- `uuid`
- `datetime`
- `pytest`

### Exports
- `TestSleepSessionPersistence`
- `TestApiBackCompat`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-24-v2-postgres-migration]] — classes: `TestSleepSessionPersistence`, `TestApiBackCompat` · methods: `test_insert_sleep_session_assigns_uuid_and_created_at`, `test_read_sleep_session_by_uuid`, `test_get_sleep_period_6m_response_shape_unchanged`, `test_sleep_session_with_stages_atomic`
- [[../../specs/2026-04-24-v2-postgres-routers-cutover]] — classes: `TestApiBackCompat` · methods: `test_get_sleep_period_6m_response_shape_unchanged`

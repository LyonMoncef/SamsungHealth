---
type: code-source
language: python
file_path: tests/server/test_scripts_csv_import.py
git_blob: 6f41d7490ad0a5cc7912d2fc88b99786a0e015c9
last_synced: '2026-04-26T18:27:45Z'
loc: 170
annotations: []
imports:
- csv
- importlib
- pathlib
- pytest
- sqlalchemy
exports:
- _write_sleep_csv
- TestImportSamsungCsv
- TestGenerateSample
tags:
- code
- python
---

# tests/server/test_scripts_csv_import.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_scripts_csv_import.py`](../../../tests/server/test_scripts_csv_import.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""
Tests RED — V2.1.2 refonte scripts vers SQLAlchemy.

Mappé sur frontmatter tested_by: tests/server/test_scripts_csv_import.py
Classes: TestImportSamsungCsv, TestGenerateSample
"""
import csv
import importlib
from pathlib import Path

import pytest
from sqlalchemy import select


SAMSUNG_SLEEP_HEADER = [
    "com.samsung.health.sleep.start_time",
    "com.samsung.health.sleep.end_time",
    "com.samsung.health.sleep.datauuid",
    "sleep_score",
    "efficiency",
    "sleep_duration",
    "sleep_cycle",
    "mental_recovery",
    "physical_recovery",
    "sleep_type",
]


def _write_sleep_csv(tmp_path: Path, rows: list[dict]) -> Path:
    """Samsung shealth CSV : line 1 vide (descriptor), line 2 header, then rows."""
    csv_path = tmp_path / "com.samsung.shealth.sleep.20260424.csv"
    with csv_path.open("w", newline="", encoding="utf-8") as f:
        f.write("\n")  # ligne descriptor ignorée par read_csv
        writer = csv.DictWriter(f, fieldnames=SAMSUNG_SLEEP_HEADER)
        writer.writeheader()
        for r in rows:
            writer.writerow(r)
    return csv_path


@pytest.fixture
def csv_export_dir(tmp_path, monkeypatch, db_session):
    """Pointe le script import_samsung_csv vers tmp_path + injecte TARGET_USER_ID."""
    import scripts.import_samsung_csv as mod
    from tests.server.conftest import _ensure_orm_default_user

    monkeypatch.setattr(mod, "EXPORT_DIR", tmp_path)
    user_id = _ensure_orm_default_user(db_session.connection())
    db_session.commit()
    monkeypatch.setattr(mod, "TARGET_USER_ID", str(user_id))
    return tmp_path


class TestImportSamsungCsv:
    def test_import_sleep_round_trip_pg(self, schema_ready, db_session, csv_export_dir):
        # spec V2.1.2 §Tests d'acceptation #1
        from server.db.models import SleepSession

        rows = [
            {
                "com.samsung.health.sleep.start_time": "2026-04-20 23:15:00.000",
                "com.samsung.health.sleep.end_time": "2026-04-21 07:30:00.000",
                "com.samsung.health.sleep.datauuid": "uuid-001",
                "sleep_score": "85",
                "efficiency": "0.92",
                "sleep_duration": "495",
                "sleep_cycle": "5",
                "mental_recovery": "0.8",
                "physical_recovery": "0.85",
                "sleep_type": "1",
            },
            {
                "com.samsung.health.sleep.start_time": "2026-04-21 22:45:00.000",
                "com.samsung.health.sleep.end_time": "2026-04-22 06:45:00.000",
                "com.samsung.health.sleep.datauuid": "uuid-002",
                "sleep_score": "78",
                "efficiency": "0.88",
                "sleep_duration": "480",
                "sleep_cycle": "4",
                "mental_recovery": "0.75",
                "physical_recovery": "0.82",
                "sleep_type": "1",
            },
        ]
        _write_sleep_csv(csv_export_dir, rows)

        from scripts.import_samsung_csv import import_sleep
        uuid_map = import_sleep(db_session)

        sessions = db_session.execute(select(SleepSession)).scalars().all()
        assert len(sessions) == 2, f"Attendu 2 sleep_sessions PG, got {len(sessions)}"
        assert isinstance(uuid_map, dict)
        assert "uuid-001" in uuid_map and "uuid-002" in uuid_map

    def test_import_idempotent_second_run_zero_inserts(self, schema_ready, db_session, csv_export_dir):
        # spec V2.1.2 §Tests d'acceptation #2
        from server.db.models import SleepSession

        rows = [
            {
                "com.samsung.health.sleep.start_time": "2026-04-22 23:00:00.000",
                "com.samsung.health.sleep.end_time": "2026-04-23 07:00:00.000",
                "com.samsung.health.sleep.datauuid": "uuid-100",
                "sleep_score": "80",
                "efficiency": "0.9",
                "sleep_duration": "480",
                "sleep_cycle": "5",
                "mental_recovery": "0.8",
                "physical_recovery": "0.8",
                "sleep_type": "1",
            },
        ]
        _write_sleep_csv(csv_export_dir, rows)

        from scripts.import_samsung_csv import import_sleep
        import_sleep(db_session)
        count_after_first = len(db_session.execute(select(SleepSession)).scalars().all())
        import_sleep(db_session)
        count_after_second = len(db_session.execute(select(SleepSession)).scalars().all())

        assert count_after_first == 1
        assert count_after_second == 1, "ON CONFLICT DO NOTHING devrait empêcher le doublon"


class TestGenerateSample:
    def test_generate_sample_creates_30d_data(self, schema_ready, db_session, monkeypatch):
        # spec V2.1.2 §Tests d'acceptation #3
        from server.db.models import HeartRateHourly, SleepSession, StepsHourly

        # Override get_session du script pour pointer vers le testcontainer
        import sys
        import scripts.generate_sample as mod
        from tests.server.conftest import _ensure_orm_default_user, _ORM_DEFAULT_USER_EMAIL

        _ensure_orm_default_user(db_session.connection())
        db_session.commit()
        monkeypatch.setattr(mod, "get_session", lambda: db_session)
        monkeypatch.setattr(sys, "argv", ["generate_sample.py", "--user-email", _ORM_DEFAULT_USER_EMAIL])

        mod.main()

        sleep_count = len(db_session.execute(select(SleepSession)).scalars().all())
        steps_count = len(db_session.execute(select(StepsHourly)).scalars().all())
        hr_count = len(db_session.execute(select(HeartRateHourly)).scalars().all())

        assert sleep_count >= 28, f"Attendu ≥ 28 sleep_sessions (30j ± weekends), got {sleep_count}"
        assert steps_count >= 30 * 12, f"Attendu ≥ 360 steps_hourly (30j × 12h actives), got {steps_count}"
        assert hr_count >= 30 * 12, f"Attendu ≥ 360 heart_rate_hourly, got {hr_count}"

    def test_generate_sample_idempotent(self, schema_ready, db_session, monkeypatch):
        # spec V2.1.2 §Tests d'acceptation #4
        from server.db.models import SleepSession

        import sys
        import scripts.generate_sample as mod
        from tests.server.conftest import _ensure_orm_default_user, _ORM_DEFAULT_USER_EMAIL

        _ensure_orm_default_user(db_session.connection())
        db_session.commit()
        monkeypatch.setattr(mod, "get_session", lambda: db_session)
        monkeypatch.setattr(sys, "argv", ["generate_sample.py", "--user-email", _ORM_DEFAULT_USER_EMAIL])

        mod.main()
        count_first = len(db_session.execute(select(SleepSession)).scalars().all())
        mod.main()
        count_second = len(db_session.execute(select(SleepSession)).scalars().all())

        assert count_first == count_second, (
            f"Idempotence violée : {count_first} → {count_second} après 2e run"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_write_sleep_csv` (function) — lines 29-38
- `TestImportSamsungCsv` (class) — lines 54-122
- `TestGenerateSample` (class) — lines 125-170

### Imports
- `csv`
- `importlib`
- `pathlib`
- `pytest`
- `sqlalchemy`

### Exports
- `_write_sleep_csv`
- `TestImportSamsungCsv`
- `TestGenerateSample`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-24-v2-csv-import-sqlalchemy]] — classes: `TestImportSamsungCsv`, `TestGenerateSample` · methods: `test_import_sleep_round_trip_pg`, `test_import_idempotent_second_run_zero_inserts`, `test_generate_sample_creates_30d_data`, `test_generate_sample_idempotent`

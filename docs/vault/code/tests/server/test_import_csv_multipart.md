---
type: code-source
language: python
file_path: tests/server/test_import_csv_multipart.py
git_blob: d022225937e4128a2da30d891fe106e8791eca75
last_synced: '2026-05-07T16:11:01Z'
loc: 825
annotations: []
imports:
- pytest
exports:
- _sleep_csv
- _heartrate_csv
- _steps_csv
- _exercise_csv
- _register_and_login
- TestAuth401
- TestMissingFilePart
- TestFileTooLarge
- TestImportSleepNominal
- TestImportSleepMalformedRow
- TestImportHeartrateNominal
- TestImportStepsNominal
- TestImportExerciseNominal
- TestImportEmptyCsv
- TestImportInvalidEncoding
- TestSecurityPathTraversal
- TestMultiUserIsolation
- TestCsvImportService
tags:
- code
- python
---

# tests/server/test_import_csv_multipart.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_import_csv_multipart.py`](../../../tests/server/test_import_csv_multipart.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Phase 4 Backend — POST /api/*/import CSV multipart endpoints.

Tests RED-first against:
- server/routers/sleep.py      (POST /api/sleep/import — à créer)
- server/routers/heartrate.py  (POST /api/heartrate/import — à créer)
- server/routers/steps.py      (POST /api/steps/import — à créer)
- server/routers/exercise.py   (POST /api/exercise/import — à créer)
- server/services/csv_import.py (module inexistant)

Spec : docs/vault/specs/2026-05-07-p4-backend-import.md §Tests d'acceptation TA-01→TA-18.

Conventions :
- Imports lazy DANS chaque test (RED clair sur module manquant)
- Commentaires `# spec: TA-XX` sur chaque test
- client_pg_ready injecte automatiquement Bearer JWT (ce fichier N'EST PAS dans _NO_AUTO_AUTH_FILES)
- Pour TA-01 (401) : forcer l'absence du header via headers={"Authorization": ""}
"""
from __future__ import annotations

import pytest

# ── constantes registration/login ─────────────────────────────────────────

_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"
_DEFAULT_PASSWORD = "longpassword12345"


# ── helpers CSV ────────────────────────────────────────────────────────────

def _sleep_csv(rows: list[str] | None = None) -> bytes:
    """Construit un CSV sleep Samsung Health minimal.

    spec: §Format CSV Samsung Health
    """
    header = "com.samsung.health.sleep.start_time,com.samsung.health.sleep.end_time"
    lines = [
        "# Samsung Health data export",
        "# com.samsung.health.sleep",
        header,
    ]
    if rows is not None:
        lines.extend(rows)
    return "\n".join(lines).encode("utf-8")


def _heartrate_csv(rows: list[str] | None = None) -> bytes:
    """Construit un CSV heart_rate Samsung Health minimal.

    spec: §Format CSV Samsung Health — heart_rate
    """
    header = (
        "com.samsung.health.heart_rate.start_time,"
        "com.samsung.health.heart_rate.heart_rate,"
        "com.samsung.health.heart_rate.min,"
        "com.samsung.health.heart_rate.max"
    )
    lines = [
        "# Samsung Health data export",
        header,
    ]
    if rows is not None:
        lines.extend(rows)
    return "\n".join(lines).encode("utf-8")


def _steps_csv(rows: list[str] | None = None) -> bytes:
    """Construit un CSV step_daily_trend Samsung Health minimal.

    spec: §Format CSV Samsung Health — steps
    """
    header = (
        "com.samsung.health.step_daily_trend.start_time,"
        "com.samsung.health.step_daily_trend.count"
    )
    lines = [
        "# Samsung Health data export",
        header,
    ]
    if rows is not None:
        lines.extend(rows)
    return "\n".join(lines).encode("utf-8")


def _exercise_csv(rows: list[str] | None = None) -> bytes:
    """Construit un CSV exercise Samsung Health minimal.

    spec: §Format CSV Samsung Health — exercise
    """
    header = (
        "com.samsung.health.exercise.start_time,"
        "com.samsung.health.exercise.end_time,"
        "com.samsung.health.exercise.exercise_type,"
        "com.samsung.health.exercise.duration"
    )
    lines = [
        "# Samsung Health data export",
        header,
    ]
    if rows is not None:
        lines.extend(rows)
    return "\n".join(lines).encode("utf-8")


def _register_and_login(client, email: str) -> str:
    """Register + login → return access_token (Bearer).

    Utilisé pour les tests multi-user (TA-18).
    """
    reg = client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": _DEFAULT_PASSWORD},
    )
    assert reg.status_code in (201, 409), f"register failed: {reg.text}"
    log = client.post("/auth/login", json={"email": email, "password": _DEFAULT_PASSWORD})
    assert log.status_code == 200, f"login failed: {log.text}"
    return log.json()["access_token"]


# ══════════════════════════════════════════════════════════════════════════
# TA-01 — Auth 401 sans token
# ══════════════════════════════════════════════════════════════════════════

class TestAuth401:

    # spec: TA-01 — JWT absent → 401
    @pytest.mark.parametrize("endpoint", [
        "/api/sleep/import",
        "/api/heartrate/import",
        "/api/steps/import",
        "/api/exercise/import",
    ])
    def test_401_without_authorization_header(self, client_pg_ready, endpoint):
        """POST sans header Authorization → 401 quel que soit l'endpoint.

        spec: TA-01 §Codes HTTP "JWT absent ou invalide → 401"
        """
        csv_bytes = _sleep_csv(["2026-04-20 23:15:00.000,2026-04-21 07:30:00.000"])
        r = client_pg_ready.post(
            endpoint,
            files={"file": ("sleep.csv", csv_bytes, "text/csv")},
            headers={"Authorization": ""},
        )
        # spec: TA-01 — 401 attendu sans token JWT
        assert r.status_code == 401, (
            f"{endpoint} sans token devrait retourner 401, got {r.status_code}"
        )


# ══════════════════════════════════════════════════════════════════════════
# TA-02 — Part `file` absent → 422
# ══════════════════════════════════════════════════════════════════════════

class TestMissingFilePart:

    # spec: TA-02 — Part file absent → 422
    def test_422_when_file_part_absent_sleep(self, client_pg_ready):
        """POST /api/sleep/import sans part `file` → 422.

        spec: TA-02 §Codes HTTP "Part file absent de la requête → 422"
        """
        r = client_pg_ready.post("/api/sleep/import")
        assert r.status_code == 422, (
            f"Part file absent devrait retourner 422, got {r.status_code}"
        )

    # spec: TA-02 — idem heartrate
    def test_422_when_file_part_absent_heartrate(self, client_pg_ready):
        """POST /api/heartrate/import sans part `file` → 422.

        spec: TA-02 §Codes HTTP "Part file absent → 422"
        """
        r = client_pg_ready.post("/api/heartrate/import")
        assert r.status_code == 422

    # spec: TA-02 — idem steps
    def test_422_when_file_part_absent_steps(self, client_pg_ready):
        """POST /api/steps/import sans part `file` → 422.

        spec: TA-02 §Codes HTTP "Part file absent → 422"
        """
        r = client_pg_ready.post("/api/steps/import")
        assert r.status_code == 422

    # spec: TA-02 — idem exercise
    def test_422_when_file_part_absent_exercise(self, client_pg_ready):
        """POST /api/exercise/import sans part `file` → 422.

        spec: TA-02 §Codes HTTP "Part file absent → 422"
        """
        r = client_pg_ready.post("/api/exercise/import")
        assert r.status_code == 422


# ══════════════════════════════════════════════════════════════════════════
# TA-03 — Fichier > 10 MB → 413
# ══════════════════════════════════════════════════════════════════════════

class TestFileTooLarge:

    # spec: TA-03 — Fichier > 10 MB → 413
    def test_413_when_file_exceeds_10mb(self, client_pg_ready):
        """POST un fichier de 10 MB + 1 octet → 413.

        spec: TA-03 §Codes HTTP "Fichier > 10 MB → 413"
        spec: §Décisions techniques "Vérification de taille avant lecture"
        MAX_CSV_BYTES = 10 * 1024 * 1024 ; si > MAX → HTTPException(413)
        """
        # 10 MB + 1 octet — la vérification de taille se fait avant le parsing
        oversized = b"x" * (10 * 1024 * 1024 + 1)
        r = client_pg_ready.post(
            "/api/sleep/import",
            files={"file": ("big.csv", oversized, "text/csv")},
        )
        # spec: TA-03 — 413 attendu
        assert r.status_code == 413, (
            f"Fichier > 10 MB devrait retourner 413, got {r.status_code}"
        )


# ══════════════════════════════════════════════════════════════════════════
# TA-04 & TA-05 — Import sleep nominal + idempotence
# ══════════════════════════════════════════════════════════════════════════

class TestImportSleepNominal:

    # spec: TA-04 — Import sleep nominal
    def test_import_sleep_nominal_returns_200_with_inserted_count(self, client_pg_ready, schema_ready, engine):
        """POST un CSV sleep valide avec 2 sessions → 200, {"inserted": 2, "skipped": 0}.

        spec: TA-04 §Tests d'acceptation "Import sleep nominal"
        spec: §Réponse JSON {"inserted": int, "skipped": int}
        """
        from sqlalchemy import select, text
        from sqlalchemy.orm import sessionmaker
        from server.db.models import SleepSession

        csv_bytes = _sleep_csv([
            "2026-04-20 23:15:00.000,2026-04-21 07:30:00.000",
            "2026-04-21 23:00:00.000,2026-04-22 06:45:00.000",
        ])
        r = client_pg_ready.post(
            "/api/sleep/import",
            files={"file": ("sleep.csv", csv_bytes, "text/csv")},
        )
        # spec: TA-04 — status 200
        assert r.status_code == 200, f"Import sleep devrait retourner 200, got {r.status_code}: {r.text}"
        body = r.json()
        # spec: TA-04 — inserted=2, skipped=0
        assert body.get("inserted") == 2, f"inserted attendu=2, got {body}"
        assert body.get("skipped") == 0, f"skipped attendu=0, got {body}"

        # Vérifier présence en DB
        Session = sessionmaker(bind=engine, expire_on_commit=False)
        with Session() as sess:
            count = sess.execute(
                select(SleepSession)
            ).scalars().all()
        assert len(count) == 2, f"2 SleepSession attendues en DB, got {len(count)}"

    # spec: TA-05 — Idempotence sleep
    def test_import_sleep_idempotent_second_import_returns_zero_inserted(self, client_pg_ready):
        """2e POST du même CSV sleep → {"inserted": 0, "skipped": 2}.

        spec: TA-05 §Tests d'acceptation "Import sleep idempotence"
        spec: §Idempotence par contrainte DB — uq_sleep_sessions_window
        """
        csv_bytes = _sleep_csv([
            "2026-04-20 23:15:00.000,2026-04-21 07:30:00.000",
            "2026-04-21 23:00:00.000,2026-04-22 06:45:00.000",
        ])
        # 1er import
        r1 = client_pg_ready.post(
            "/api/sleep/import",
            files={"file": ("sleep.csv", csv_bytes, "text/csv")},
        )
        assert r1.status_code == 200
        # 2e import — doit être idempotent
        r2 = client_pg_ready.post(
            "/api/sleep/import",
            files={"file": ("sleep.csv", csv_bytes, "text/csv")},
        )
        assert r2.status_code == 200, f"2e import devrait retourner 200, got {r2.status_code}"
        body = r2.json()
        # spec: TA-05 — inserted=0, skipped=2
        assert body.get("inserted") == 0, f"inserted attendu=0 (idempotence), got {body}"
        assert body.get("skipped") == 2, f"skipped attendu=2 (idempotence), got {body}"


# ══════════════════════════════════════════════════════════════════════════
# TA-06 — Ligne malformée ignorée
# ══════════════════════════════════════════════════════════════════════════

class TestImportSleepMalformedRow:

    # spec: TA-06 — Ligne malformée comptée dans skipped
    def test_import_sleep_malformed_row_is_skipped(self, client_pg_ready):
        """CSV avec 2 lignes valides + 1 start_time invalide → {"inserted": 2, "skipped": 1}.

        spec: TA-06 §Tests d'acceptation "Import sleep ligne malformée ignorée"
        spec: §Format CSV "une ligne malformée est loguée et comptée comme skipped"
        """
        csv_bytes = _sleep_csv([
            "2026-04-20 23:15:00.000,2026-04-21 07:30:00.000",
            "not-a-date,2026-04-22 06:45:00.000",   # ligne malformée
            "2026-04-22 22:00:00.000,2026-04-23 06:00:00.000",
        ])
        r = client_pg_ready.post(
            "/api/sleep/import",
            files={"file": ("sleep.csv", csv_bytes, "text/csv")},
        )
        assert r.status_code == 200, f"Import avec ligne malformée devrait retourner 200, got {r.status_code}"
        body = r.json()
        # spec: TA-06 — inserted=2, skipped=1
        assert body.get("inserted") == 2, f"inserted attendu=2, got {body}"
        assert body.get("skipped") == 1, f"skipped attendu=1 (ligne malformée), got {body}"


# ══════════════════════════════════════════════════════════════════════════
# TA-07 & TA-08 — Import heartrate nominal + idempotence
# ══════════════════════════════════════════════════════════════════════════

class TestImportHeartrateNominal:

    # spec: TA-07 — Import heartrate nominal + agrégation horaire
    def test_import_heartrate_nominal_aggregates_by_hour(self, client_pg_ready, schema_ready, engine):
        """CSV heart_rate avec 4 samples heure-1 et 2 samples heure-2 → 2 HeartRateHourly en DB.

        spec: TA-07 §Tests d'acceptation "Import heartrate nominal + agrégation horaire"
        spec: §Logique d'agrégation heart_rate — grouper par (date, hour), sample_count = len(groupe)
        """
        from sqlalchemy import select
        from sqlalchemy.orm import sessionmaker
        from server.db.models import HeartRateHourly

        csv_bytes = _heartrate_csv([
            # 4 samples à 22h
            "2026-04-20 22:00:00.000,72,58,89",
            "2026-04-20 22:10:00.000,70,56,85",
            "2026-04-20 22:20:00.000,74,60,91",
            "2026-04-20 22:45:00.000,68,55,82",
            # 2 samples à 23h
            "2026-04-20 23:05:00.000,65,52,78",
            "2026-04-20 23:40:00.000,67,54,80",
        ])
        r = client_pg_ready.post(
            "/api/heartrate/import",
            files={"file": ("hr.csv", csv_bytes, "text/csv")},
        )
        # spec: TA-07 — status 200
        assert r.status_code == 200, f"Import heartrate devrait retourner 200, got {r.status_code}: {r.text}"
        body = r.json()
        # spec: TA-07 — inserted=2 (2 slots horaires), skipped=0
        assert body.get("inserted") == 2, f"inserted attendu=2 (2 slots), got {body}"
        assert body.get("skipped") == 0, f"skipped attendu=0, got {body}"

        # Vérifier en DB : 2 HeartRateHourly avec sample_count correct
        Session = sessionmaker(bind=engine, expire_on_commit=False)
        with Session() as sess:
            rows = sess.execute(
                select(HeartRateHourly).order_by(HeartRateHourly.hour)
            ).scalars().all()
        assert len(rows) == 2, f"2 HeartRateHourly attendus en DB, got {len(rows)}"
        counts = sorted(r.sample_count for r in rows)
        # spec: TA-07 — sample_count=4 pour 22h, sample_count=2 pour 23h
        assert counts == [2, 4], f"sample_counts attendus=[2, 4], got {counts}"

    # spec: TA-08 — Idempotence heartrate
    def test_import_heartrate_idempotent_second_import_returns_zero_inserted(self, client_pg_ready):
        """2e POST du même CSV heartrate → {"inserted": 0, "skipped": 2}.

        spec: TA-08 §Tests d'acceptation "Import heartrate idempotence"
        spec: §Idempotence — pg_insert on_conflict_do_nothing uq_hr_hourly_slot
        """
        csv_bytes = _heartrate_csv([
            "2026-04-20 22:00:00.000,72,58,89",
            "2026-04-20 22:10:00.000,70,56,85",
            "2026-04-20 22:20:00.000,74,60,91",
            "2026-04-20 22:45:00.000,68,55,82",
            "2026-04-20 23:05:00.000,65,52,78",
            "2026-04-20 23:40:00.000,67,54,80",
        ])
        # 1er import
        r1 = client_pg_ready.post(
            "/api/heartrate/import",
            files={"file": ("hr.csv", csv_bytes, "text/csv")},
        )
        assert r1.status_code == 200
        # 2e import
        r2 = client_pg_ready.post(
            "/api/heartrate/import",
            files={"file": ("hr.csv", csv_bytes, "text/csv")},
        )
        assert r2.status_code == 200
        body = r2.json()
        # spec: TA-08 — inserted=0, skipped=2 (2 slots déjà en DB)
        assert body.get("inserted") == 0, f"inserted attendu=0, got {body}"
        assert body.get("skipped") == 2, f"skipped attendu=2, got {body}"


# ══════════════════════════════════════════════════════════════════════════
# TA-09 & TA-10 — Import steps nominal + idempotence
# ══════════════════════════════════════════════════════════════════════════

class TestImportStepsNominal:

    # spec: TA-09 — Import steps nominal + agrégation horaire
    def test_import_steps_nominal_aggregates_by_hour(self, client_pg_ready, schema_ready, engine):
        """CSV steps avec 3 lignes dans la même heure → 1 StepsHourly avec step_count = somme.

        spec: TA-09 §Tests d'acceptation "Import steps nominal + agrégation horaire"
        spec: §Logique d'agrégation steps — step_count = sum(count) sur le slot horaire
        """
        from sqlalchemy import select
        from sqlalchemy.orm import sessionmaker
        from server.db.models import StepsHourly

        csv_bytes = _steps_csv([
            "2026-04-20 10:00:00.000,1200",
            "2026-04-20 10:15:00.000,800",
            "2026-04-20 10:45:00.000,600",
        ])
        r = client_pg_ready.post(
            "/api/steps/import",
            files={"file": ("steps.csv", csv_bytes, "text/csv")},
        )
        # spec: TA-09 — status 200
        assert r.status_code == 200, f"Import steps devrait retourner 200, got {r.status_code}: {r.text}"
        body = r.json()
        # spec: TA-09 — inserted=1 (1 slot horaire), skipped=0
        assert body.get("inserted") == 1, f"inserted attendu=1, got {body}"
        assert body.get("skipped") == 0, f"skipped attendu=0, got {body}"

        # Vérifier en DB : step_count = 1200 + 800 + 600 = 2600
        Session = sessionmaker(bind=engine, expire_on_commit=False)
        with Session() as sess:
            rows = sess.execute(select(StepsHourly)).scalars().all()
        assert len(rows) == 1, f"1 StepsHourly attendu en DB, got {len(rows)}"
        assert rows[0].step_count == 2600, (
            f"step_count attendu=2600 (1200+800+600), got {rows[0].step_count}"
        )

    # spec: TA-10 — Idempotence steps
    def test_import_steps_idempotent_second_import_returns_zero_inserted(self, client_pg_ready):
        """2e POST du même CSV steps → {"inserted": 0, "skipped": 1}.

        spec: TA-10 §Tests d'acceptation "Import steps idempotence"
        spec: §Idempotence — pg_insert on_conflict_do_nothing uq_steps_hourly_slot
        """
        csv_bytes = _steps_csv([
            "2026-04-20 10:00:00.000,1200",
            "2026-04-20 10:15:00.000,800",
            "2026-04-20 10:45:00.000,600",
        ])
        r1 = client_pg_ready.post(
            "/api/steps/import",
            files={"file": ("steps.csv", csv_bytes, "text/csv")},
        )
        assert r1.status_code == 200
        r2 = client_pg_ready.post(
            "/api/steps/import",
            files={"file": ("steps.csv", csv_bytes, "text/csv")},
        )
        assert r2.status_code == 200
        body = r2.json()
        # spec: TA-10 — inserted=0, skipped=1 (1 slot déjà en DB)
        assert body.get("inserted") == 0, f"inserted attendu=0, got {body}"
        assert body.get("skipped") == 1, f"skipped attendu=1, got {body}"


# ══════════════════════════════════════════════════════════════════════════
# TA-11, TA-12, TA-13 — Import exercise nominal + type inconnu + idempotence
# ══════════════════════════════════════════════════════════════════════════

class TestImportExerciseNominal:

    # spec: TA-11 — Import exercise nominal avec mapping exercise_type
    def test_import_exercise_nominal_maps_exercise_type(self, client_pg_ready, schema_ready, engine):
        """CSV exercise avec type 1001 (running) et 1007 (walking) → 2 ExerciseSession en DB.

        spec: TA-11 §Tests d'acceptation "Import exercise nominal"
        spec: §Mapping exercise_type Samsung → string métier
        """
        from sqlalchemy import select
        from sqlalchemy.orm import sessionmaker
        from server.db.models import ExerciseSession

        csv_bytes = _exercise_csv([
            "2026-04-20 07:00:00.000,2026-04-20 07:45:00.000,1001,2700000",
            "2026-04-20 18:00:00.000,2026-04-20 18:30:00.000,1007,1800000",
        ])
        r = client_pg_ready.post(
            "/api/exercise/import",
            files={"file": ("exercise.csv", csv_bytes, "text/csv")},
        )
        # spec: TA-11 — status 200
        assert r.status_code == 200, f"Import exercise devrait retourner 200, got {r.status_code}: {r.text}"
        body = r.json()
        # spec: TA-11 — inserted=2, skipped=0
        assert body.get("inserted") == 2, f"inserted attendu=2, got {body}"
        assert body.get("skipped") == 0, f"skipped attendu=0, got {body}"

        # Vérifier mapping exercise_type en DB
        Session = sessionmaker(bind=engine, expire_on_commit=False)
        with Session() as sess:
            rows = sess.execute(
                select(ExerciseSession).order_by(ExerciseSession.exercise_start)
            ).scalars().all()
        assert len(rows) == 2, f"2 ExerciseSession attendues en DB, got {len(rows)}"
        types = [r.exercise_type for r in rows]
        # spec: TA-11 — 1001→"running", 1007→"walking"
        assert "running" in types, f"exercise_type 'running' attendu, got {types}"
        assert "walking" in types, f"exercise_type 'walking' attendu, got {types}"

    # spec: TA-12 — Type inconnu → "samsung_<code>"
    def test_import_exercise_unknown_type_stored_as_samsung_code(self, client_pg_ready, schema_ready, engine):
        """CSV exercise avec code inconnu (9999) → exercise_type="samsung_9999".

        spec: TA-12 §Tests d'acceptation "Import exercise type inconnu"
        spec: §Mapping exercise_type "Pour tout code inconnu : exercise_type = f"samsung_{code}""
        """
        from sqlalchemy import select
        from sqlalchemy.orm import sessionmaker
        from server.db.models import ExerciseSession

        csv_bytes = _exercise_csv([
            "2026-04-20 10:00:00.000,2026-04-20 10:30:00.000,9999,1800000",
        ])
        r = client_pg_ready.post(
            "/api/exercise/import",
            files={"file": ("exercise.csv", csv_bytes, "text/csv")},
        )
        assert r.status_code == 200, f"Import exercise type inconnu devrait retourner 200, got {r.status_code}"
        body = r.json()
        # spec: TA-12 — inserted=1, skipped=0
        assert body.get("inserted") == 1, f"inserted attendu=1, got {body}"
        assert body.get("skipped") == 0, f"skipped attendu=0, got {body}"

        Session = sessionmaker(bind=engine, expire_on_commit=False)
        with Session() as sess:
            rows = sess.execute(select(ExerciseSession)).scalars().all()
        assert len(rows) == 1
        # spec: TA-12 — code inconnu → "samsung_9999"
        assert rows[0].exercise_type == "samsung_9999", (
            f"exercise_type attendu='samsung_9999', got {rows[0].exercise_type!r}"
        )

    # spec: TA-13 — Idempotence exercise
    def test_import_exercise_idempotent_second_import_returns_zero_inserted(self, client_pg_ready):
        """2e POST du même CSV exercise → {"inserted": 0, "skipped": 2}.

        spec: TA-13 §Tests d'acceptation "Import exercise idempotence"
        spec: §Idempotence — pg_insert on_conflict_do_nothing uq_exercise_window
        """
        csv_bytes = _exercise_csv([
            "2026-04-20 07:00:00.000,2026-04-20 07:45:00.000,1001,2700000",
            "2026-04-20 18:00:00.000,2026-04-20 18:30:00.000,1007,1800000",
        ])
        r1 = client_pg_ready.post(
            "/api/exercise/import",
            files={"file": ("exercise.csv", csv_bytes, "text/csv")},
        )
        assert r1.status_code == 200
        r2 = client_pg_ready.post(
            "/api/exercise/import",
            files={"file": ("exercise.csv", csv_bytes, "text/csv")},
        )
        assert r2.status_code == 200
        body = r2.json()
        # spec: TA-13 — inserted=0, skipped=2
        assert body.get("inserted") == 0, f"inserted attendu=0, got {body}"
        assert body.get("skipped") == 2, f"skipped attendu=2, got {body}"


# ══════════════════════════════════════════════════════════════════════════
# TA-14 — CSV vide (header + 0 lignes de données)
# ══════════════════════════════════════════════════════════════════════════

class TestImportEmptyCsv:

    # spec: TA-14 — CSV vide (0 lignes de données)
    def test_import_sleep_empty_csv_returns_zero_inserted(self, client_pg_ready):
        """CSV avec lignes # + header mais 0 données → 200, {"inserted": 0, "skipped": 0}.

        spec: TA-14 §Tests d'acceptation "CSV vide (0 lignes de données)"
        spec: §Codes HTTP "CSV entièrement malformé (0 lignes parsées) → 200 {inserted:0, skipped:0}"
        """
        # rows=[] → seulement le header, pas de données
        csv_bytes = _sleep_csv(rows=[])
        r = client_pg_ready.post(
            "/api/sleep/import",
            files={"file": ("sleep_empty.csv", csv_bytes, "text/csv")},
        )
        assert r.status_code == 200, f"CSV vide devrait retourner 200, got {r.status_code}"
        body = r.json()
        # spec: TA-14 — inserted=0, skipped=0
        assert body.get("inserted") == 0, f"inserted attendu=0, got {body}"
        assert body.get("skipped") == 0, f"skipped attendu=0, got {body}"

    # spec: TA-15 — CSV entièrement composé de lignes # (pas de header)
    def test_import_sleep_only_comment_lines_returns_zero(self, client_pg_ready):
        """CSV uniquement composé de lignes # → 200, {"inserted": 0, "skipped": 0}.

        spec: TA-15 §Tests d'acceptation "CSV entièrement # (pas de header)"
        spec: §Format CSV "toute ligne dont le premier caractère est # est ignorée"
        """
        csv_bytes = b"# Samsung Health data export\n# com.samsung.health.sleep\n# Another comment\n"
        r = client_pg_ready.post(
            "/api/sleep/import",
            files={"file": ("sleep_comments_only.csv", csv_bytes, "text/csv")},
        )
        assert r.status_code == 200, f"CSV tout-commentaires devrait retourner 200, got {r.status_code}"
        body = r.json()
        # spec: TA-15 — inserted=0, skipped=0, pas d'exception
        assert body.get("inserted") == 0, f"inserted attendu=0, got {body}"
        assert body.get("skipped") == 0, f"skipped attendu=0, got {body}"


# ══════════════════════════════════════════════════════════════════════════
# TA-16 — Encodage non-UTF-8 → 422
# ══════════════════════════════════════════════════════════════════════════

class TestImportInvalidEncoding:

    # spec: TA-16 — Fichier binaire non-UTF-8 → 422
    def test_import_sleep_non_utf8_binary_returns_422(self, client_pg_ready):
        """Fichier binaire non-UTF-8 → 422 avec detail "invalid_csv_encoding".

        spec: TA-16 §Tests d'acceptation "Encodage non-UTF-8"
        spec: §Sécurité "parse_samsung_csv tente décodage UTF-8 ; si UnicodeDecodeError → 422"
        """
        # Séquence d'octets invalide UTF-8
        non_utf8 = b"\xff\xfe\x00\x01invalid binary content \x80\x90\xa0"
        r = client_pg_ready.post(
            "/api/sleep/import",
            files={"file": ("bad.csv", non_utf8, "application/octet-stream")},
        )
        # spec: TA-16 — 422 attendu
        assert r.status_code == 422, (
            f"Fichier non-UTF-8 devrait retourner 422, got {r.status_code}"
        )
        # spec: TA-16 — detail "invalid_csv_encoding"
        detail = r.json().get("detail", "")
        assert "invalid_csv_encoding" in str(detail), (
            f"detail attendu='invalid_csv_encoding', got {detail!r}"
        )


# ══════════════════════════════════════════════════════════════════════════
# TA-17 — Sécurité : path traversal via filename ignoré
# ══════════════════════════════════════════════════════════════════════════

class TestSecurityPathTraversal:

    # spec: TA-17 — Filename path traversal ignoré
    def test_path_traversal_filename_is_ignored_and_returns_200(self, client_pg_ready):
        """CSV valide avec filename="../../../etc/passwd" → 200 (contenu traité, filesystem non touché).

        spec: TA-17 §Tests d'acceptation "Sécurité : filename avec path traversal"
        spec: §Sécurité "Le filename n'est jamais utilisé pour accéder au filesystem"
        spec: §Décisions techniques "Sécurité filename"
        """
        csv_bytes = _sleep_csv(["2026-04-20 23:15:00.000,2026-04-21 07:30:00.000"])
        r = client_pg_ready.post(
            "/api/sleep/import",
            files={"file": ("../../../etc/passwd", csv_bytes, "text/csv")},
        )
        # spec: TA-17 — 200 attendu (le contenu est traité normalement)
        assert r.status_code == 200, (
            f"Filename path traversal devrait être ignoré, got {r.status_code}: {r.text}"
        )
        body = r.json()
        # La réponse doit contenir inserted+skipped (shape normale)
        assert "inserted" in body, f"Réponse doit contenir 'inserted', got {body}"
        assert "skipped" in body, f"Réponse doit contenir 'skipped', got {body}"


# ══════════════════════════════════════════════════════════════════════════
# TA-18 — Isolation user_id (multi-user)
# ══════════════════════════════════════════════════════════════════════════

class TestMultiUserIsolation:

    # spec: TA-18 — Isolation user_id : chaque user a ses propres SleepSession
    def test_two_users_import_same_csv_have_independent_sleep_sessions(
        self, client_pg_ready, schema_ready, engine
    ):
        """User A et User B importent le même CSV sleep → sessions distinctes par user_id.

        spec: TA-18 §Tests d'acceptation "Isolation user_id"
        spec: §Contrainte DB uq_sleep_sessions_window (user_id, sleep_start, sleep_end)
        L'unicité est par (user_id, start, end) — deux users différents peuvent avoir
        la même plage sans conflit.
        """
        from sqlalchemy import select
        from sqlalchemy.orm import sessionmaker
        from server.db.models import SleepSession

        csv_bytes = _sleep_csv(["2026-04-20 23:15:00.000,2026-04-21 07:30:00.000"])

        # User A : récupérer le token déjà injecté par client_pg_ready
        token_a = client_pg_ready.headers.get("Authorization", "").replace("Bearer ", "")

        # User B : register + login un second user
        token_b = _register_and_login(client_pg_ready, "user-b-multiuser@samsunghealth.local")

        # Import par User A
        r_a = client_pg_ready.post(
            "/api/sleep/import",
            files={"file": ("sleep.csv", csv_bytes, "text/csv")},
            headers={"Authorization": f"Bearer {token_a}"},
        )
        assert r_a.status_code == 200, f"Import user A failed: {r_a.text}"
        assert r_a.json().get("inserted") == 1

        # Import par User B avec le même CSV
        r_b = client_pg_ready.post(
            "/api/sleep/import",
            files={"file": ("sleep.csv", csv_bytes, "text/csv")},
            headers={"Authorization": f"Bearer {token_b}"},
        )
        assert r_b.status_code == 200, f"Import user B failed: {r_b.text}"
        # spec: TA-18 — user B doit aussi avoir inserted=1 (pas de collision avec user A)
        assert r_b.json().get("inserted") == 1, (
            f"User B devrait insérer 1 session indépendamment de user A, got {r_b.json()}"
        )

        # Vérifier en DB : 2 SleepSession avec user_id distincts
        Session = sessionmaker(bind=engine, expire_on_commit=False)
        with Session() as sess:
            all_sessions = sess.execute(select(SleepSession)).scalars().all()
        assert len(all_sessions) == 2, (
            f"2 SleepSession attendues en DB (1 par user), got {len(all_sessions)}"
        )
        user_ids = {str(s.user_id) for s in all_sessions}
        # spec: TA-18 — user_id distincts (isolation)
        assert len(user_ids) == 2, (
            f"Les 2 SleepSession doivent avoir des user_id distincts, got {user_ids}"
        )


# ══════════════════════════════════════════════════════════════════════════
# Tests unitaires du service csv_import (module inexistant → RED)
# ══════════════════════════════════════════════════════════════════════════

class TestCsvImportService:
    """Tests unitaires du module server.services.csv_import.

    spec: §Contrats d'interface — service partagé server/services/csv_import.py
    Ces tests vérifient les helpers purs sans dépendances FastAPI.
    """

    # spec: §parse_samsung_csv — ignore lignes #, retourne list[dict]
    def test_parse_samsung_csv_ignores_comment_lines(self):
        """parse_samsung_csv ignore les lignes # et retourne les données comme list[dict].

        spec: §Décisions techniques "parse_samsung_csv(raw_bytes) → list[dict]"
        spec: §Format CSV "toute ligne dont le premier caractère est # est ignorée"
        """
        from server.services.csv_import import parse_samsung_csv  # noqa: PLC0415

        raw = (
            b"# Samsung Health data export\n"
            b"# com.samsung.health.sleep\n"
            b"com.samsung.health.sleep.start_time,com.samsung.health.sleep.end_time\n"
            b"2026-04-20 23:15:00.000,2026-04-21 07:30:00.000\n"
        )
        rows = parse_samsung_csv(raw)
        assert isinstance(rows, list), f"parse_samsung_csv doit retourner une list, got {type(rows)}"
        assert len(rows) == 1, f"1 ligne de données attendue, got {len(rows)}"
        assert "com.samsung.health.sleep.start_time" in rows[0], (
            f"Clé start_time absente, got keys={list(rows[0].keys())}"
        )

    # spec: §parse_samsung_csv — CSV vide (que des #)
    def test_parse_samsung_csv_returns_empty_list_for_comments_only(self):
        """parse_samsung_csv sur CSV tout-commentaires → list vide.

        spec: §parse_samsung_csv "ignore les lignes #, retourne rows comme list[dict]"
        """
        from server.services.csv_import import parse_samsung_csv  # noqa: PLC0415

        raw = b"# Samsung Health data export\n# com.samsung.health.sleep\n"
        rows = parse_samsung_csv(raw)
        assert rows == [], f"CSV tout-commentaires → list vide attendue, got {rows}"

    # spec: §parse_samsung_csv — UnicodeDecodeError → doit lever (ou convertir en exception métier)
    def test_parse_samsung_csv_raises_on_non_utf8(self):
        """parse_samsung_csv sur bytes non-UTF-8 lève une exception (UnicodeDecodeError ou ValueError).

        spec: §Sécurité "parse_samsung_csv tente décodage UTF-8 ; si UnicodeDecodeError → HTTPException(422)"
        Au niveau service, l'exception brute est soit UnicodeDecodeError soit une ValueError métier.
        """
        from server.services.csv_import import parse_samsung_csv  # noqa: PLC0415

        non_utf8 = b"\xff\xfe\x80\x90 invalid"
        with pytest.raises((UnicodeDecodeError, ValueError)):
            parse_samsung_csv(non_utf8)

    # spec: §MAX_CSV_BYTES = 10 * 1024 * 1024
    def test_max_csv_bytes_constant_is_10_mb(self):
        """La constante MAX_CSV_BYTES doit valoir exactement 10 * 1024 * 1024.

        spec: §Contrats d'interface "MAX_CSV_BYTES = 10 * 1024 * 1024"
        """
        from server.services.csv_import import MAX_CSV_BYTES  # noqa: PLC0415

        assert MAX_CSV_BYTES == 10 * 1024 * 1024, (
            f"MAX_CSV_BYTES attendu={10 * 1024 * 1024}, got {MAX_CSV_BYTES}"
        )

    # spec: §parse_samsung_csv — header présent mais 0 lignes données
    def test_parse_samsung_csv_empty_data_section_returns_empty_list(self):
        """parse_samsung_csv sur CSV avec header mais sans lignes données → list vide.

        spec: §parse_samsung_csv — header + 0 données → list vide
        """
        from server.services.csv_import import parse_samsung_csv  # noqa: PLC0415

        raw = (
            b"# Samsung Health data export\n"
            b"com.samsung.health.sleep.start_time,com.samsung.health.sleep.end_time\n"
        )
        rows = parse_samsung_csv(raw)
        assert rows == [], f"Header seul → list vide attendue, got {rows}"
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_sleep_csv` (function) — lines 30-43
- `_heartrate_csv` (function) — lines 46-63
- `_steps_csv` (function) — lines 66-81
- `_exercise_csv` (function) — lines 84-101
- `_register_and_login` (function) — lines 104-117
- `TestAuth401` (class) — lines 124-147
- `TestMissingFilePart` (class) — lines 154-192
- `TestFileTooLarge` (class) — lines 199-218
- `TestImportSleepNominal` (class) — lines 225-287
- `TestImportSleepMalformedRow` (class) — lines 294-316
- `TestImportHeartrateNominal` (class) — lines 323-398
- `TestImportStepsNominal` (class) — lines 405-468
- `TestImportExerciseNominal` (class) — lines 475-572
- `TestImportEmptyCsv` (class) — lines 579-616
- `TestImportInvalidEncoding` (class) — lines 623-646
- `TestSecurityPathTraversal` (class) — lines 653-675
- `TestMultiUserIsolation` (class) — lines 682-739
- `TestCsvImportService` (class) — lines 746-825

### Imports
- `pytest`

### Exports
- `_sleep_csv`
- `_heartrate_csv`
- `_steps_csv`
- `_exercise_csv`
- `_register_and_login`
- `TestAuth401`
- `TestMissingFilePart`
- `TestFileTooLarge`
- `TestImportSleepNominal`
- `TestImportSleepMalformedRow`
- `TestImportHeartrateNominal`
- `TestImportStepsNominal`
- `TestImportExerciseNominal`
- `TestImportEmptyCsv`
- `TestImportInvalidEncoding`
- `TestSecurityPathTraversal`
- `TestMultiUserIsolation`
- `TestCsvImportService`

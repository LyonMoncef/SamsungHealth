---
type: code-source
language: python
file_path: tests/server/test_mood_encryption.py
git_blob: a169abb50d91d4c32717e9f1b3cb0cbf0d1ae4c8
last_synced: '2026-04-24T03:32:00Z'
loc: 149
annotations: []
imports:
- datetime
- pytest
exports:
- TestMoodPersistenceEncrypted
- TestMoodApiBackCompat
- TestMoodErrorSanitization
tags:
- code
- python
---

# tests/server/test_mood_encryption.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_mood_encryption.py`](../../../tests/server/test_mood_encryption.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""
Tests RED — V2.2 chiffrement table mood (round-trip + back-compat + sanitization).

Mappé sur frontmatter tested_by: tests/server/test_mood_encryption.py
Classes: TestMoodPersistenceEncrypted, TestMoodApiBackCompat, TestMoodErrorSanitization
"""
from datetime import datetime, timezone

import pytest


class TestMoodPersistenceEncrypted:
    def test_mood_notes_stored_as_bytes_in_pg(self, schema_ready, db_session):
        # spec V2.2 §11
        from sqlalchemy import text

        from server.db.models import Mood

        secret = "donnée santé ultra confidentielle"
        m = Mood(
            start_time=datetime(2026, 4, 24, 10, 0, tzinfo=timezone.utc),
            notes=secret,
        )
        db_session.add(m)
        db_session.commit()

        # Query SQL brute → bytes, pas de plaintext
        row = db_session.execute(text("SELECT notes FROM mood LIMIT 1")).fetchone()
        raw = row[0]
        assert isinstance(raw, (bytes, memoryview)), f"Attendu bytes/memoryview en BYTEA, got {type(raw)}"
        raw_bytes = bytes(raw)
        assert secret.encode("utf-8") not in raw_bytes, "Le plaintext UTF-8 ne doit JAMAIS être visible en clair en DB"

    def test_mood_round_trip_via_orm_transparent(self, schema_ready, db_session):
        # spec V2.2 §12
        from sqlalchemy import select

        from server.db.models import Mood

        m = Mood(
            start_time=datetime(2026, 4, 24, 11, 0, tzinfo=timezone.utc),
            mood_type=4,
            notes="texte libre",
            emotions="joie",
            factors="famille",
        )
        db_session.add(m)
        db_session.commit()
        db_session.expire_all()

        loaded = db_session.execute(select(Mood)).scalar_one()
        assert loaded.mood_type == 4
        assert loaded.notes == "texte libre"
        assert loaded.emotions == "joie"
        assert loaded.factors == "famille"

    def test_mood_crypto_v_column_initialised_to_1(self, schema_ready, db_session):
        # spec V2.2 §13
        from sqlalchemy import text

        from server.db.models import Mood

        db_session.add(Mood(start_time=datetime(2026, 4, 24, 12, 0, tzinfo=timezone.utc), notes="x"))
        db_session.commit()

        row = db_session.execute(
            text("SELECT notes_crypto_v, emotions_crypto_v, factors_crypto_v, mood_type_crypto_v FROM mood LIMIT 1")
        ).fetchone()
        assert tuple(row) == (1, 1, 1, 1), f"Toutes les _crypto_v à 1, got {tuple(row)}"


class TestMoodApiBackCompat:
    def test_post_get_mood_round_trip(self, schema_ready, client_pg):
        # spec V2.2 §14
        payload = {
            "entries": [
                {
                    "start_time": "2026-04-24T13:00:00+00:00",
                    "mood_type": 3,
                    "notes": "journée calme",
                    "emotions": "sérénité",
                    "factors": "météo",
                    "place": "home",
                    "company": "solo",
                },
                {
                    "start_time": "2026-04-24T18:00:00+00:00",
                    "mood_type": 5,
                    "notes": "soir énergique",
                    "emotions": "joie,fierté",
                    "factors": "sport,amis",
                    "place": "park",
                    "company": "friends",
                },
            ]
        }
        r = client_pg.post("/api/mood", json=payload)
        assert r.status_code == 201, r.text
        assert r.json() == {"inserted": 2, "skipped": 0}

        r = client_pg.get("/api/mood")
        assert r.status_code == 200
        rows = r.json()
        assert len(rows) == 2
        first = next(x for x in rows if x["start_time"].startswith("2026-04-24T13"))
        assert first["mood_type"] == 3
        assert first["notes"] == "journée calme"
        assert first["emotions"] == "sérénité"
        assert first["factors"] == "météo"

    def test_mood_response_shape_unchanged(self, schema_ready, client_pg):
        # spec V2.2 §15
        client_pg.post("/api/mood", json={"entries": [{
            "start_time": "2026-04-24T14:00:00+00:00",
            "mood_type": 2,
            "notes": "neutre",
        }]})
        r = client_pg.get("/api/mood")
        assert r.status_code == 200
        first = r.json()[0]
        expected_keys = {"start_time", "mood_type", "emotions", "factors", "notes", "place", "company"}
        missing = expected_keys - set(first.keys())
        assert not missing, f"Clés manquantes : {missing}"
        # Pas de bytes leak
        for k, v in first.items():
            assert v is None or isinstance(v, (str, int)), f"{k}={v!r} doit être str/int/None — pas de bytes leak"


class TestMoodErrorSanitization:
    def test_decrypt_failure_returns_500_generic(self, schema_ready, client_pg, db_session):
        # spec V2.2 §16
        from sqlalchemy import text

        from server.db.models import Mood

        # Insert valide via ORM
        db_session.add(Mood(start_time=datetime(2026, 4, 24, 15, 0, tzinfo=timezone.utc), notes="legit"))
        db_session.commit()

        # Tamper : flip un byte dans le ciphertext stocké
        db_session.execute(text("UPDATE mood SET notes = decode('00112233445566778899aabbccddeeff', 'hex')"))
        db_session.commit()

        r = client_pg.get("/api/mood")
        assert r.status_code == 500, f"Attendu 500, got {r.status_code}: {r.text}"
        body = r.text.lower()
        for forbidden in ("invalidtag", "aes", "gcm", "tampered", "key", "decrypt", "crypto"):
            assert forbidden not in body, f"Mot interdit '{forbidden}' leak dans la response : {body}"
        assert "internal_decryption_error" in r.json().get("detail", "")
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestMoodPersistenceEncrypted` (class) — lines 12-69
- `TestMoodApiBackCompat` (class) — lines 72-126
- `TestMoodErrorSanitization` (class) — lines 129-149

### Imports
- `datetime`
- `pytest`

### Exports
- `TestMoodPersistenceEncrypted`
- `TestMoodApiBackCompat`
- `TestMoodErrorSanitization`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields]] — classes: `TestMoodPersistenceEncrypted`, `TestMoodApiBackCompat`, `TestMoodErrorSanitization` · methods: `test_mood_notes_stored_as_bytes_in_pg`, `test_mood_round_trip_via_orm_transparent`, `test_mood_crypto_v_column_initialised_to_1`, `test_post_get_mood_round_trip`, `test_mood_response_shape_unchanged`, `test_decrypt_failure_returns_500_generic`

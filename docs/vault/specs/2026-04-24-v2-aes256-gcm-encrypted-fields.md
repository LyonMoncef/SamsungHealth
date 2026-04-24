---
type: spec
title: "V2.2 — Chiffrement AES-256-GCM at-rest (fondation + table pilote mood)"
slug: 2026-04-24-v2-aes256-gcm-encrypted-fields
status: ready
created: 2026-04-24
delivered: null
priority: high
related_plans:
  - 2026-04-23-plan-v2-refactor-master
related_specs:
  - 2026-04-24-v2-postgres-migration
implements:
  - file: server/security/__init__.py
    symbols: []
  - file: server/security/crypto.py
    symbols: [load_encryption_key, encrypt_field, decrypt_field, EncryptionConfigError, DecryptionError]
  - file: server/db/encrypted.py
    symbols: [EncryptedBytes, EncryptedString, EncryptedInt]
  - file: server/db/models.py
    symbols: [Mood]
  - file: server/main.py
    symbols: [app, _validate_encryption_at_boot]
  - file: server/routers/mood.py
    symbols: [router, create_mood_entry, get_mood_entries]
  - file: server/models.py
    symbols: [MoodIn, MoodOut, MoodBulkIn]
  - file: alembic/versions/0002_encrypt_mood.py
    symbols: [upgrade, downgrade]
tested_by:
  - file: tests/server/test_crypto_foundation.py
    classes: [TestLoadEncryptionKey, TestEncryptDecryptField, TestEncryptedTypeDecorator, TestBootValidation]
    methods:
      - test_load_key_from_env
      - test_load_key_missing_env_raises
      - test_load_key_invalid_base64_raises
      - test_load_key_wrong_length_raises
      - test_load_key_all_zeros_raises
      - test_round_trip_bytes
      - test_round_trip_string
      - test_tamper_detected_invalid_tag
      - test_typedecorator_transparent_on_orm
      - test_app_boot_fails_fast_without_key
  - file: tests/server/test_mood_encryption.py
    classes: [TestMoodPersistenceEncrypted, TestMoodApiBackCompat, TestMoodErrorSanitization]
    methods:
      - test_mood_notes_stored_as_bytes_in_pg
      - test_mood_round_trip_via_orm_transparent
      - test_mood_crypto_v_column_initialised_to_1
      - test_post_get_mood_round_trip
      - test_mood_response_shape_unchanged
      - test_decrypt_failure_returns_500_generic
tags: [v2.2, security, encryption, aes-gcm, rgpd, art9, foundation]
---

# Spec — V2.2 Chiffrement AES-256-GCM at-rest (fondation + table pilote mood)

## Vision

Premier vrai chiffrement applicatif des champs santé Art.9 du RGPD. Cette spec pose la **fondation** (helper crypto + `Encrypted*` `TypeDecorator` SQLAlchemy + validation clé fail-fast au boot + sanitization erreurs) et la valide sur **une table pilote** : `mood` (texte libre intime — `notes`, `emotions`, `factors` + `mood_type` smallint).

Une fois mergée, la spec V2.2.1 étendra le pattern aux 9 autres tables Art.9 (sleep_sessions, weight, blood_pressure, stress, spo2, heart_rate_hourly, respiratory_rate, skin_temperature, ecg) sans changement structurel — copy-paste du pattern.

## Décisions techniques

- **AES-256-GCM** via `cryptography.hazmat.primitives.ciphers.aead.AESGCM` (lib standard, audited, FIPS-validated)
- **Clé maître unique en env var `SAMSUNGHEALTH_ENCRYPTION_KEY`** (base64 32 bytes). Pas de KMS/Vault/HSM en V2.2 (différé V2.x — Out of scope)
- **Validation stricte au boot** (`server/main.py` startup avant `app.include_router`) :
  1. Présence env var → sinon `EncryptionConfigError` au boot
  2. Format base64 valide → sinon raise
  3. Décodée = exactement 32 bytes → sinon raise
  4. Non-zéro (pas `b"\x00" * 32`) → sinon raise (signe d'init manquant ou config par défaut dangereuse)
- **Stockage `BYTEA` brut** — `nonce(12) || ciphertext_with_tag(N)` concaténés en bytes, sans JSON wrapper. Compact + pas de leak de schéma. AESGCM API gère déjà le tag automatiquement dans le ciphertext output.
- **`Encrypted*` `TypeDecorator` SQLAlchemy** — transparent à l'ORM (cohérent avec `Uuid7` V2.1) :
  - `EncryptedBytes` (cas générique — bytes/str manipulés en clair côté Python)
  - `EncryptedString` (sérialise str ↔ utf-8 bytes avant/après chiffrement)
  - `EncryptedInt` (sérialise int ↔ str → utf-8 bytes — utile pour `mood_type` smallint qui devient sensible une fois lié à une note)
- **Versionning du chiffrement** : pour chaque colonne chiffrée X, ajouter colonne `<X>_crypto_v: smallint` (default `1`). Permet la rotation future sans perte (V2.x = ajouter `_crypto_v=2` + déchiffrer-rechiffrer ligne par ligne).
- **Sanitization erreurs déchiffrement** : si `cryptography.exceptions.InvalidTag` ou autre erreur déchiffrement, le router catch et renvoie `500 {"detail": "internal_decryption_error"}` générique. Jamais de leak (clé tournée ? tampering ? data corrompue ? on ne dit pas).
- **Pas de fallback en clair** — si la clé manque au boot, le serveur ne démarre pas du tout (RuntimeError fail-fast). Le contraire serait une faille (data servie sans chiffrement attendu).
- **`.env.example`** template versionné, **`.env`** gitignored (vérifier — déjà fait via `.gitignore` `.env`).
- **Pas de migration de données existantes** : fresh DB assumée (cohérent V2.1). Si données mood existent, l'utilisateur doit les ré-importer.

## Livrables

- [ ] `server/security/__init__.py` (package vide)
- [ ] `server/security/crypto.py` — `load_encryption_key() -> bytes` (lit env, valide), `encrypt_field(plaintext: bytes) -> bytes`, `decrypt_field(ciphertext: bytes) -> bytes`, exceptions `EncryptionConfigError` et `DecryptionError`
- [ ] `server/db/encrypted.py` — `EncryptedBytes`, `EncryptedString`, `EncryptedInt` `TypeDecorator` SQLAlchemy
- [ ] `server/db/models.py` — `Mood` migré : `notes`/`emotions`/`factors` en `EncryptedString` (BYTEA storage), `mood_type` en `EncryptedInt`. Ajout colonnes `notes_crypto_v`, `emotions_crypto_v`, `factors_crypto_v`, `mood_type_crypto_v` (smallint, default 1, NOT NULL)
- [ ] `alembic/versions/0002_encrypt_mood.py` — migration : `op.alter_column` sur les 4 colonnes (TEXT → BYTEA, INT → BYTEA), ajout des 4 colonnes `_crypto_v`. Downgrade documenté comme **non-data-preserving** (raise ou no-op selon stratégie)
- [ ] `server/main.py` — fonction `_validate_encryption_at_boot()` appelée AVANT `app.include_router`. Si raise → propagation, app ne démarre pas
- [ ] `server/routers/mood.py` — nouveau router (n'existait pas en V2.1) — POST `/api/mood` (single + bulk), GET `/api/mood?from=...&to=...`. Utilise `Depends(get_session)` + `Mood` ORM. Wrap les exceptions de déchiffrement en `HTTPException(500, "internal_decryption_error")` générique
- [ ] `server/models.py` — `MoodIn`, `MoodOut`, `MoodBulkIn` (Pydantic, types python natifs str/int) + inclusion router dans `server/main.py`
- [ ] `requirements.txt` — `cryptography>=42.0` (lib AES-GCM)
- [ ] `.env.example` — template avec `SAMSUNGHEALTH_ENCRYPTION_KEY=<base64-32-bytes-here>` + commentaire de génération `python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"`
- [ ] `README.md` — section sécurité ajoutée : génération clé + sauvegarde hors repo + warning fresh DB
- [ ] `tests/server/conftest.py` — fixture autouse `_set_test_encryption_key` qui set `SAMSUNGHEALTH_ENCRYPTION_KEY` à une clé valide pour les tests (clé test connue ≠ clé prod)

## Tests d'acceptation

### `TestLoadEncryptionKey`
1. **Load OK** — `test_load_key_from_env` : env var = base64(32 bytes random), `load_encryption_key()` retourne les 32 bytes décodés
2. **Fail si absente** — `test_load_key_missing_env_raises` : env var absente → `EncryptionConfigError`
3. **Fail si invalid base64** — `test_load_key_invalid_base64_raises` : env var = "not_base64!@#" → `EncryptionConfigError`
4. **Fail si mauvaise longueur** — `test_load_key_wrong_length_raises` : env var = base64(16 bytes) → `EncryptionConfigError("expected 32 bytes, got 16")`
5. **Fail si all-zero** — `test_load_key_all_zeros_raises` : env var = base64(`b"\x00" * 32`) → `EncryptionConfigError("default/zero key forbidden")`

### `TestEncryptDecryptField`
6. **Round-trip bytes** — `test_round_trip_bytes` : `decrypt_field(encrypt_field(b"hello")) == b"hello"`
7. **Round-trip string** — `test_round_trip_string` (via TypeDecorator EncryptedString) : insert "Bonjour 🌙", read = "Bonjour 🌙"
8. **Tampering détecté** — `test_tamper_detected_invalid_tag` : flip 1 byte du ciphertext → `decrypt_field` raise `DecryptionError` (wrap de `cryptography.exceptions.InvalidTag`)

### `TestEncryptedTypeDecorator`
9. **Transparent à l'ORM** — `test_typedecorator_transparent_on_orm` : insert via SQLAlchemy `Mood(notes="x")`, query → `mood.notes == "x"`. L'ORM ne voit jamais les bytes chiffrés

### `TestBootValidation`
10. **App boot fails fast sans clé** — `test_app_boot_fails_fast_without_key` : monkeypatch.delenv → import `server.main` → `EncryptionConfigError`

### `TestMoodPersistenceEncrypted`
11. **BYTEA en DB** — `test_mood_notes_stored_as_bytes_in_pg` : insert notes="secret", query SQL brute via psycopg `SELECT notes FROM mood` → `bytes` qui ne contient PAS "secret" en clair
12. **Round-trip ORM** — `test_mood_round_trip_via_orm_transparent` : insert + read via ORM → notes/emotions/factors/mood_type identiques aux valeurs envoyées
13. **`_crypto_v = 1`** — `test_mood_crypto_v_column_initialised_to_1` : after insert, query crypto_v → 1

### `TestMoodApiBackCompat`
14. **POST/GET round-trip** — `test_post_get_mood_round_trip` : POST `/api/mood {"start_time": "...", "notes": "x", "mood_type": 3}` → GET → JSON identique
15. **Shape JSON inchangé** — `test_mood_response_shape_unchanged` : GET `/api/mood` retourne liste avec clés `start_time`, `mood_type`, `emotions`, `factors`, `notes`, `place`, `company` — types python natifs (str, int, None) — pas de bytes leak

### `TestMoodErrorSanitization`
16. **500 générique sur tampering** — `test_decrypt_failure_returns_500_generic` : tamper directement BYTEA en DB via SQL brute, GET `/api/mood` → response 500 avec body `{"detail": "internal_decryption_error"}`. Le mot "InvalidTag", "AES", "GCM", "tampered", "key" ne doivent JAMAIS apparaître dans la response

## Suite naturelle

**Spec V2.2.1** — étendre le pattern aux 9 autres tables Art.9 :
- `sleep_sessions` (sleep_score, efficiency, sleep_duration_min, sleep_cycle, mental_recovery, physical_recovery, sleep_type)
- `weight` (weight_kg, body_fat_pct, skeletal_muscle_pct, skeletal_muscle_mass_kg, fat_free_mass_kg, basal_metabolic_rate, total_body_water_kg)
- `blood_pressure` (systolic, diastolic, pulse, mean_bp)
- `stress` (score)
- `spo2` (spo2, min_spo2, max_spo2, low_duration_s)
- `heart_rate_hourly` (min_bpm, max_bpm, avg_bpm) — attention impact perf sur volumes (~700 rows/jour)
- `respiratory_rate` (average, lower_limit, upper_limit)
- `skin_temperature` (temperature, min_temp, max_temp)
- `ecg` (mean_heart_rate, classification)
- Migration Alembic `0003_encrypt_remaining_art9.py` qui rejoue le pattern de `0002`
- Suite naturelle de V2.2.1 : V2.3 JWT auth (peut consommer la fondation crypto pour signer les refresh tokens si besoin)

## Out of scope V2.2

- KMS / AWS Secrets Manager / Vault — V2.x, après stabilisation
- Séparation KEK / DEK — différé
- Rotation automatique de clé — V2.x (mais le versionning `_crypto_v` est posé dès V2.2)
- HSM (Hardware Security Module) — out of scope total
- Migration des données SQLite existantes — fresh DB
- Chiffrement des autres tables Art.9 — spec V2.2.1
- Chiffrement des `created_at`/`updated_at` — non sensibles (timestamps métier)
- Chiffrement de l'identifiant UUID v7 — non sensible (timestamp-sortable mais pas data santé)

---
type: spec
title: "V2.2.1 — Étendre chiffrement AES-256-GCM aux 9 tables Art.9 restantes"
slug: 2026-04-24-v2-aes256-gcm-extend-art9
status: ready
created: 2026-04-24
delivered: null
priority: high
related_plans:
  - 2026-04-23-plan-v2-refactor-master
related_specs:
  - 2026-04-24-v2-aes256-gcm-encrypted-fields
implements:
  - file: server/db/encrypted.py
    symbols: [EncryptedFloat]
  - file: server/db/models.py
    symbols: [SleepSession, Weight, BloodPressure, Stress, Spo2, HeartRateHourly, RespiratoryRate, SkinTemperature, Ecg]
  - file: alembic/versions/0003_encrypt_remaining_art9.py
    symbols: [upgrade, downgrade]
tested_by:
  - file: tests/server/test_encryption_extend_art9.py
    classes: [TestSentinelleBytea, TestRoundTripCritique]
    methods:
      - test_sleep_sessions_art9_columns_are_bytea
      - test_weight_art9_columns_are_bytea
      - test_blood_pressure_art9_columns_are_bytea
      - test_stress_score_is_bytea
      - test_spo2_art9_columns_are_bytea
      - test_heart_rate_hourly_art9_columns_are_bytea
      - test_respiratory_rate_art9_columns_are_bytea
      - test_skin_temperature_art9_columns_are_bytea
      - test_ecg_art9_columns_are_bytea
      - test_sleep_score_round_trip_via_orm
      - test_weight_kg_round_trip_via_orm
      - test_crypto_v_columns_default_to_1_everywhere
tags: [v2.2.1, security, encryption, aes-gcm, rgpd, art9, extension]
---

# Spec — V2.2.1 Étendre chiffrement aux 9 tables Art.9 restantes

## Vision

V2.2 a livré la fondation crypto + table pilote `mood`. V2.2.1 applique le même pattern transparent aux 9 autres tables santé Art.9 du schéma. Pas de changement structurel — copy-paste mécanique du pattern V2.2 (typage `Encrypted*` + colonne `_crypto_v` + migration Alembic).

## Décisions techniques

- **Ajout `EncryptedFloat` au TypeDecorator** dans `server/db/encrypted.py` — V2.2 ne couvrait que `EncryptedBytes`/`String`/`Int`. Sérialisation `repr(float).encode('ascii')` (préserve la précision IEEE 754, contrairement à `str(float)` qui peut tronquer) et `float(decoded)` au déchiffrement.
- **9 tables, 33 colonnes Art.9 chiffrées + 33 colonnes `_crypto_v`** :
  - `sleep_sessions` : `sleep_score` (Int), `efficiency` (Float), `sleep_duration_min` (Int), `sleep_cycle` (Int), `mental_recovery` (Float), `physical_recovery` (Float), `sleep_type` (Int) — 7 cols
  - `weight` : `weight_kg`, `body_fat_pct`, `skeletal_muscle_pct`, `skeletal_muscle_mass_kg`, `fat_free_mass_kg` (Float ×5) + `basal_metabolic_rate` (Int) + `total_body_water_kg` (Float) — 7 cols
  - `blood_pressure` : `systolic`, `diastolic`, `mean_bp` (Float ×3) + `pulse` (Int) — 4 cols
  - `stress` : `score` (Float) — 1 col
  - `spo2` : `spo2`, `min_spo2`, `max_spo2` (Float ×3) + `low_duration_s` (Int) — 4 cols
  - `heart_rate_hourly` : `min_bpm`, `max_bpm`, `avg_bpm` (Int ×3) — 3 cols
  - `respiratory_rate` : `average`, `lower_limit`, `upper_limit` (Float ×3) — 3 cols
  - `skin_temperature` : `temperature`, `min_temp`, `max_temp` (Float ×3) — 3 cols
  - `ecg` : `mean_heart_rate` (Float) + `classification` (Int) — 2 cols
- **Migration Alembic 0003** unique pour les 9 tables (pas une migration par table — éviter l'overhead). `postgresql_using='NULL'` partout (fresh DB assumée, cohérent V2.2).
- **`tag_id`/`sample_count`/`spo2.tag_id` non chiffrés** : ce sont des références techniques (sample_count = nb mesures = métadonnée non sensible, tag_id = id technique).
- **Champs non Art.9 conservés en clair** : `start_time`/`end_time`/`date`/`hour`/`day_date`/`exercise_type` (timestamps + types d'événement = métier, non Art.9).
- **Pas de touche aux routers** : transparent via TypeDecorator. Les tests round-trip routers V2.1.1 doivent continuer à passer 0 modif.
- **Pas de touche à `import_samsung_csv.py` / `generate_sample.py`** : les `pg_insert(Model).values(...)` envoient des Python natifs (str/int/float), TypeDecorator chiffre transparent. Les `_upsert(...)` continuent à fonctionner.

## Livrables

- [ ] `server/db/encrypted.py` — ajout `EncryptedFloat` `TypeDecorator`
- [ ] `server/db/models.py` — 9 tables patchées (33 colonnes `Encrypted*` + 33 colonnes `_crypto_v`)
- [ ] `alembic/versions/0003_encrypt_remaining_art9.py` — migration `op.add_column` ×33 + `op.alter_column` ×33 (avec `postgresql_using='NULL'`). Downgrade documenté non-data-preserving
- [ ] `tests/server/test_encryption_extend_art9.py` — 9 sentinelles BYTEA + 2 round-trip critiques (sleep_score Int + weight_kg Float) + 1 sentinelle `_crypto_v` default
- [ ] V2.2 spec marquée `delivered` mise à jour avec lien vers V2.2.1

## Tests d'acceptation

### `TestSentinelleBytea` (9 tests, pattern : insert ORM + query SQL brute → bytes ≠ valeur clair)
1-9. **Pour chacune des 9 tables** : insert via ORM avec valeur connue, query `SELECT <col> FROM <table>` via SQL brute → `bytes` retourné, et le pattern bytes du plaintext (ex: `b'85'` pour `sleep_score=85`) ne doit PAS apparaître dans les bytes.

### `TestRoundTripCritique`
10. **`test_sleep_score_round_trip_via_orm`** : insert `SleepSession(sleep_score=85, efficiency=0.92)`, read via ORM → `sleep_score == 85` (Int round-trip OK), `efficiency == 0.92` (Float round-trip OK)
11. **`test_weight_kg_round_trip_via_orm`** : insert `Weight(weight_kg=72.5, body_fat_pct=18.3, basal_metabolic_rate=1650)`, read → tous identiques (Float + Int mixés OK)
12. **`test_crypto_v_columns_default_to_1_everywhere`** : insert un row dans chaque table, query `SELECT *_crypto_v FROM <table>` → toutes les valeurs = 1

## Suite naturelle

V2.2 fondation chiffrement Art.9 = **complète** une fois V2.2.1 mergée (spec parente V2.2 reste `delivered`, V2.2.1 = extension).

Spec V2.3 (à créer) : **JWT auth multi-user** + `Depends(get_current_user)` sur tous les endpoints. Quand multi-user, prévoir colonne `user_id: UUID` sur toutes les tables (FK vers `users`) — pas dans V2.2.1 (single-user assumé).

## Out of scope V2.2.1

- KMS / Vault / HSM — V2.x
- Rotation de clé — V2.x (versionning posé)
- Migration des données existantes — fresh DB
- Index sur colonnes chiffrées (impossible en l'état car BYTEA opaques) — V2.x si besoin (HMAC index ?)
- Optimisations perf bulk insert pour `heart_rate_hourly` (~700 rows/jour × 3 chiffrements) — différé, à mesurer en prod

---
title: "Phase 4 Backend — Endpoints import CSV multipart"
slug: 2026-05-07-p4-backend-import
status: ready
created: 2026-05-07
phase: P4
branch: feat/p4-backend-import
tags: [backend, fastapi, csv, android, import, multipart, rgpd]
implements:
  - server/routers/sleep.py
  - server/routers/heartrate.py
  - server/routers/steps.py
  - server/routers/exercise.py
  - server/services/csv_import.py
tested_by:
  - tests/server/test_import_csv_multipart.py
parent_specs: []
---

# Spec — Phase 4 Backend — Endpoints import CSV multipart

## Vision

Les endpoints `/api/*/import` permettent à l'app Android (Phase 4) d'envoyer directement un fichier CSV Samsung Health via une requête multipart POST, sans passer par le script `scripts/import_samsung_csv.py`. Ce canal remplace le flux batch CLI pour les imports mobiles : l'utilisateur sélectionne un ZIP Samsung Health sur l'appareil, l'app en extrait le CSV pertinent et le pousse vers le backend. Le backend parse, déduplique et stocke — le tout sans jamais écrire le fichier sur disque, sans jamais le retransmettre, et en passant obligatoirement par les ORM models (chiffrement EncryptedField garanti).

## Contexte technique

**Backend existant.** FastAPI synchrone (handlers `def`, pas `async def`). SQLAlchemy ORM synchrone. Tous les routers cibles (`sleep.py`, `heartrate.py`, `steps.py`, `exercise.py`) suivent le même pattern : auth via `Depends(get_current_user)`, rate-limit via `@limiter.limit(_api_post_cap, key_func=_user_id_key)`, réponse `{"inserted": int, "skipped": int}`.

**Pas de migration Alembic.** Les tables cibles existent déjà avec leurs contraintes UNIQUE servant à l'idempotence :

| Table | Contrainte unique |
|---|---|
| `sleep_sessions` | `uq_sleep_sessions_window` (`user_id`, `sleep_start`, `sleep_end`) |
| `sleep_stages` | `uq_sleep_stages_window` (`user_id`, `stage_start`, `stage_end`) |
| `heart_rate_hourly` | `uq_hr_hourly_slot` (`user_id`, `date`, `hour`) |
| `steps_hourly` | `uq_steps_hourly_slot` (`user_id`, `date`, `hour`) |
| `exercise_sessions` | `uq_exercise_window` (`user_id`, `exercise_start`, `exercise_end`) |

**Chiffrement Art.9.** Les champs `min_bpm`/`max_bpm`/`avg_bpm` (HeartRateHourly), `sleep_score`/`efficiency`/... (SleepSession), et les champs ExerciseSession non-chiffrés selon le modèle actuel. Les endpoints import écrivent via les ORM models sans bypass — le chiffrement est transparent.

**Client Android.** Part multipart nommé `"file"`, Content-Type `text/csv` ou `application/octet-stream`, un seul fichier par requête, taille typique 10 Ko–2 Mo (export annuel). Interface Retrofit :

```kotlin
@Multipart
@POST("api/sleep/import")
suspend fun importSleep(@Part file: MultipartBody.Part): ImportApiResponse

data class ImportApiResponse(val inserted: Int, val skipped: Int)
```

## Format CSV Samsung Health

Les CSV exportés par Samsung Health ont la structure suivante :

```
# Ligne de métadonnées (commence par #, ignorée)
# Une ou plusieurs lignes # supplémentaires possibles
com.samsung.health.sleep.start_time,com.samsung.health.sleep.end_time,...
2026-04-20 23:15:00.000,2026-04-21 07:30:00.000,...
```

**Règle de parsing :** toute ligne dont le premier caractère est `#` est ignorée. La première ligne non-`#` est le header CSV. Les lignes suivantes sont les données. Le parser est tolérant : une ligne malformée (champ manquant, date invalide, valeur non castable) est loguée et comptée comme `skipped`, sans lever d'exception globale.

### Colonnes par type (noms exacts dans le CSV Samsung Health)

**sleep** (`com.samsung.health.sleep*.csv`) :
- `com.samsung.health.sleep.start_time` — ISO8601 ou `YYYY-MM-DD HH:MM:SS.mmm`
- `com.samsung.health.sleep.end_time` — même format
- `sleep_score` — entier optionnel
- `efficiency` — float optionnel (0.0–1.0)
- `sleep_duration` — entier optionnel (minutes)
- `sleep_cycle` — entier optionnel
- `mental_recovery` — float optionnel
- `physical_recovery` — float optionnel
- `sleep_type` — entier optionnel

**heart_rate** (`com.samsung.health.heart_rate*.csv`) :
- `com.samsung.health.heart_rate.start_time` — timestamp
- `com.samsung.health.heart_rate.heart_rate` — bpm moyen (int)
- `com.samsung.health.heart_rate.min` — bpm min (int)
- `com.samsung.health.heart_rate.max` — bpm max (int)
- Le parser agrège par slot horaire (`date` = `YYYY-MM-DD`, `hour` = 0–23) via moyenne/min/max et `sample_count` = nombre de lignes dans le slot.

**steps** (`com.samsung.health.step_daily_trend*.csv`) :
- `com.samsung.health.step_daily_trend.start_time` — timestamp
- `com.samsung.health.step_daily_trend.count` — step_count (int)
- Agrégation par slot horaire (même logique que heart_rate).

**exercise** (`com.samsung.health.exercise*.csv`) :
- `com.samsung.health.exercise.start_time` — timestamp
- `com.samsung.health.exercise.end_time` — timestamp
- `com.samsung.health.exercise.exercise_type` — entier Samsung → mappage vers string métier (voir §Mapping exercise_type)
- `com.samsung.health.exercise.duration` — durée en ms → convertir en minutes (float)

### Mapping exercise_type Samsung → string métier

Le CSV Samsung Health encode l'activité physique par un entier. Exemples connus :

| Code int | String métier |
|---|---|
| 1001 | `running` |
| 1002 | `cycling` |
| 1007 | `walking` |
| 1008 | `hiking` |
| 3000 | `swimming` |
| 90001 | `indoor_cycling` |

Pour tout code inconnu : `exercise_type = f"samsung_{code}"`. Le parser ne doit jamais lever d'exception sur un code inconnu.

## Décisions techniques

1. **Traitement en mémoire uniquement.** `file.read()` → décodage UTF-8 → `io.StringIO` → `csv.DictReader`. Aucun `tempfile`, aucun `open()` vers le système de fichiers.

2. **Vérification de taille avant lecture.** FastAPI `UploadFile` expose la taille dans le Content-Length de la requête, mais ce header est optionnel. La lecture se fait avec une limite dure : lire jusqu'à 10 MB + 1 octet ; si la lecture dépasse 10 MB → lever `HTTPException(413)` immédiatement, sans committer.

3. **Service partagé `server/services/csv_import.py`.** Contient les helpers purs (sans dépendances FastAPI) :
   - `parse_samsung_csv(raw_bytes: bytes) -> list[dict]` — décode, ignore les `#`, retourne les lignes comme dicts.
   - `parse_sleep_rows(rows, user_id, db) -> tuple[int, int]` — insère/déduplique, retourne `(inserted, skipped)`.
   - `parse_heartrate_rows(rows, user_id, db) -> tuple[int, int]`
   - `parse_steps_rows(rows, user_id, db) -> tuple[int, int]`
   - `parse_exercise_rows(rows, user_id, db) -> tuple[int, int]`

4. **Idempotence par contrainte DB.** sleep utilise le pattern `select` + `on_conflict do_nothing` (même logique que le `POST /api/sleep` existant). heartrate, steps, exercise utilisent `pg_insert(...).on_conflict_do_nothing(...)` (même pattern que les bulk JSON existants).

5. **Sécurité filename.** Le `filename` du `Content-Disposition` n'est jamais utilisé pour accéder au système de fichiers. Il peut être logué (tronqué à 255 chars) mais n'influence aucun chemin d'accès.

6. **Injection CSV.** Les valeurs parsées sont passées uniquement via les paramètres ORM SQLAlchemy — jamais interpolées dans une requête SQL brute. Le `csv.DictReader` ne peut pas déclencher d'injection SQL.

7. **Logging structuré.** Chaque import logué avec `_log.info("csv_import.done", endpoint=..., inserted=..., skipped=..., user_id=...)`. Les lignes ignorées sont accumulées pendant le parsing puis résumées en un seul `_log.warning("csv_import.rows_skipped", count=N, reasons=Counter({...}), user_id=...)` en fin de traitement — jamais un log par ligne (évite le volume et limite la répétition du user_id dans les logs).

8. **Réponse 200 (pas 201).** L'opération est idempotente par construction. La convention `201 Created` s'applique quand la ressource est garantie nouvelle — ici elle peut être `0 inserted / N skipped`.

9. **Rate-limit réutilisé.** `@limiter.limit(_api_post_cap, key_func=_user_id_key)` — même cap que les POST bulk JSON existants (1000/heure par user). Pas de cap séparé pour l'import CSV.

10. **Pas d'agrégation côté Android.** Le backend agrège lui-même les lignes heart_rate et steps par slot horaire — l'Android envoie le CSV brut tel qu'exporté par Samsung Health.

## Livrables

- [ ] `server/services/csv_import.py` — parser CSV Samsung Health + 4 fonctions d'insertion
- [ ] `server/routers/sleep.py` — ajouter `POST /api/sleep/import` (fonction `import_sleep`)
- [ ] `server/routers/heartrate.py` — ajouter `POST /api/heartrate/import` (fonction `import_heartrate`)
- [ ] `server/routers/steps.py` — ajouter `POST /api/steps/import` (fonction `import_steps`)
- [ ] `server/routers/exercise.py` — ajouter `POST /api/exercise/import` (fonction `import_exercise`)
- [ ] `tests/server/test_import_csv_multipart.py` — 18 tests d'acceptation (voir §Tests d'acceptation)

## Contrats d'interface

### Signature des 4 endpoints

```python
# Dans chaque router — exemple pour sleep :
@router.post("/import", status_code=200)
@limiter.limit(_api_post_cap, key_func=_user_id_key)
def import_sleep(
    request: Request,
    file: UploadFile = File(...),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict:
    ...
    return {"inserted": int, "skipped": int}
```

Le part multipart est reçu via `file: UploadFile = File(...)`. FastAPI mappe automatiquement le part nommé `"file"` du client Android.

### Signature du service partagé

```python
# server/services/csv_import.py

MAX_CSV_BYTES = 10 * 1024 * 1024  # 10 MB

def parse_samsung_csv(raw_bytes: bytes) -> list[dict]:
    """Décode UTF-8, ignore les lignes #, retourne rows comme list[dict]."""
    ...

def parse_sleep_rows(
    rows: list[dict],
    user_id: UUID,
    db: Session,
) -> tuple[int, int]:
    """Insère SleepSession + SleepStage. Retourne (inserted, skipped)."""
    ...

def parse_heartrate_rows(
    rows: list[dict],
    user_id: UUID,
    db: Session,
) -> tuple[int, int]:
    """Agrège par slot horaire, pg_insert on_conflict_do_nothing. Retourne (inserted, skipped)."""
    ...

def parse_steps_rows(
    rows: list[dict],
    user_id: UUID,
    db: Session,
) -> tuple[int, int]:
    """Agrège par slot horaire, pg_insert on_conflict_do_nothing. Retourne (inserted, skipped)."""
    ...

def parse_exercise_rows(
    rows: list[dict],
    user_id: UUID,
    db: Session,
) -> tuple[int, int]:
    """Mappe exercise_type int → string, pg_insert on_conflict_do_nothing. Retourne (inserted, skipped)."""
    ...
```

### Réponse JSON

```json
{"inserted": 3, "skipped": 1}
```

`inserted + skipped` = nombre de lignes de données valides parsées (hors lignes `#` et header). Une ligne avec une date invalide est `skipped`. Une ligne dont la paire `(start, end)` existe déjà en DB est `skipped`.

### Codes HTTP

| Cas | Code |
|---|---|
| Import réussi (même si 0 inserts) | 200 |
| JWT absent ou invalide | 401 |
| Fichier > 10 MB | 413 |
| Part `file` absent de la requête | 422 |
| CSV entièrement malformé (0 lignes parsées) | 200 avec `{"inserted": 0, "skipped": 0}` |
| Rate limit dépassé | 429 |

## Logique d'agrégation heart_rate par slot horaire

Le CSV Samsung Health enregistre une mesure par sample (plusieurs par heure). Le modèle DB `HeartRateHourly` stocke un agrégat par heure. Algorithme :

1. Parser toutes les lignes valides en `(timestamp, bpm, min_bpm, max_bpm)`.
2. Grouper par `(date=YYYY-MM-DD, hour=HH)`.
3. Pour chaque groupe : `avg_bpm = round(mean(bpm))`, `min_bpm = min(min_bpm)`, `max_bpm = max(max_bpm)`, `sample_count = len(groupe)`.
4. `pg_insert(HeartRateHourly).values(...).on_conflict_do_nothing(index_elements=["user_id", "date", "hour"])`.

Même logique pour `StepsHourly` : `step_count = sum(count)` sur toutes les lignes du slot horaire.

## Logique sleep : pas de pg_insert direct

`SleepSession` utilise un `select` préalable (pattern du `POST /api/sleep` existant) car les stages associées nécessitent le `session.id` nouvellement créé. Séquence pour chaque ligne CSV :

1. Vérifier existence via `select(SleepSession).where(user_id=..., sleep_start=..., sleep_end=...)`.
2. Si trouvée : `skipped += 1`, continuer.
3. Sinon : `db.add(SleepSession(...))`, `db.flush()` pour obtenir l'id, puis `db.add(SleepStage(...))` pour chaque stage si présente dans le CSV.
4. `inserted += 1`.
5. `db.commit()` une seule fois à la fin (après la boucle complète).

Note : le CSV sleep Samsung Health ne contient pas les stages dans le même fichier. Les stages ne sont donc pas insérées par l'endpoint import — seulement la `SleepSession`. Cette contrainte est documentée ici pour éviter toute tentation d'invention.

## Sécurité (périmètre pentester C3)

| Vecteur | Mitigation |
|---|---|
| Path traversal via `filename` du Content-Disposition | Le filename n'est jamais utilisé pour accéder au filesystem — il est uniquement logué (tronqué à 255 chars). |
| Injection CSV (formules `=CMD()`) | Les valeurs sont passées via paramètres ORM SQLAlchemy — aucun SQL interpolé, aucun rendu tableur. |
| Taille fichier (Zip Bomb légère) | Lecture limitée à MAX_CSV_BYTES (10 MB) avec early return 413. |
| Fichier non-CSV (binaire, ZIP, exécutable) | `parse_samsung_csv` tente le décodage UTF-8 ; si `UnicodeDecodeError` → `HTTPException(422, "invalid_csv_encoding")`. |
| Authentification absente | `Depends(get_current_user)` retourne 401 avant d'atteindre le handler. |
| Rate-limit self-DoS | Même cap `_api_post_cap` (1000/heure/user) que les POST bulk. |
| Content-Type non validé | Le backend ne valide pas le Content-Type du part (Android peut envoyer `application/octet-stream`) — seul le contenu est parsé. |

## Logging

Chaque import produit exactement un log `INFO` en fin de traitement :

```python
_log.info(
    "csv_import.done",
    endpoint="sleep",
    inserted=inserted,
    skipped=skipped,
    user_id=str(current_user.id),
    filename=file.filename[:255] if file.filename else None,
)
```

Les lignes ignorées sont accumulées dans un `Counter` par catégorie (`"missing_field"`, `"invalid_date"`, `"invalid_value"`) et produisent un seul log `WARNING` agrégé en fin de traitement (jamais un log par ligne) :

```python
_log.warning(
    "csv_import.rows_skipped",
    endpoint="sleep",
    count=sum(skip_reasons.values()),
    reasons=dict(skip_reasons),  # {"invalid_date": 2, "missing_field": 1}
    user_id=str(current_user.id),
)
```

## Tests d'acceptation

Le fichier `tests/server/test_import_csv_multipart.py` doit couvrir les cas suivants avec la fixture `client_pg_ready` (TestClient + schema PG migré + Bearer injecté). Les helpers CSV doivent construire des bytes qui imitent exactement le format Samsung Health (lignes `#` + header + données).

---

**TA-01 — Auth 401 sans token**
Given aucun cookie/header `Authorization`, When `POST /api/sleep/import` avec un CSV valide, Then `401`.

**TA-02 — Part `file` absent**
Given un client authentifié, When `POST /api/sleep/import` sans part `file`, Then `422`.

**TA-03 — Fichier > 10 MB**
Given un client authentifié, When `POST /api/sleep/import` avec un fichier de 10 MB + 1 octet, Then `413`.

**TA-04 — Import sleep nominal**
Given un CSV sleep valide avec 2 sessions (lignes `#` + header + 2 lignes données), When `POST /api/sleep/import`, Then `200`, `{"inserted": 2, "skipped": 0}`, et 2 `SleepSession` en DB avec `user_id` correct.

**TA-05 — Import sleep idempotence**
Given le CSV de TA-04 déjà importé, When `POST /api/sleep/import` avec le même CSV, Then `200`, `{"inserted": 0, "skipped": 2}`, et toujours 2 `SleepSession` en DB (pas de doublon).

**TA-06 — Import sleep ligne malformée ignorée**
Given un CSV sleep de 3 lignes dont 1 avec `start_time` invalide (`"not-a-date"`), When `POST /api/sleep/import`, Then `200`, `{"inserted": 2, "skipped": 1}`.

**TA-07 — Import heartrate nominal + agrégation horaire**
Given un CSV heart_rate avec 4 samples sur la même heure et 2 sur l'heure suivante, When `POST /api/heartrate/import`, Then `200`, `{"inserted": 2, "skipped": 0}`, et 2 `HeartRateHourly` en DB avec `sample_count=4` et `sample_count=2`.

**TA-08 — Import heartrate idempotence**
Given le CSV de TA-07 déjà importé, When `POST /api/heartrate/import` avec le même CSV, Then `200`, `{"inserted": 0, "skipped": 2}`.

**TA-09 — Import steps nominal + agrégation horaire**
Given un CSV steps avec 3 lignes dans la même heure, When `POST /api/steps/import`, Then `200`, `{"inserted": 1, "skipped": 0}`, et 1 `StepsHourly` avec `step_count` = somme des 3 valeurs.

**TA-10 — Import steps idempotence**
Given le CSV de TA-09 déjà importé, When `POST /api/steps/import` avec le même CSV, Then `200`, `{"inserted": 0, "skipped": 1}`.

**TA-11 — Import exercise nominal**
Given un CSV exercise avec 2 sessions (type 1001 et 1007), When `POST /api/exercise/import`, Then `200`, `{"inserted": 2, "skipped": 0}`, et 2 `ExerciseSession` en DB avec `exercise_type="running"` et `exercise_type="walking"`.

**TA-12 — Import exercise type inconnu**
Given un CSV exercise avec un type inconnu (code 9999), When `POST /api/exercise/import`, Then `200`, `{"inserted": 1, "skipped": 0}`, et `exercise_type="samsung_9999"`.

**TA-13 — Import exercise idempotence**
Given le CSV de TA-11 déjà importé, When `POST /api/exercise/import` avec le même CSV, Then `200`, `{"inserted": 0, "skipped": 2}`.

**TA-14 — CSV vide (0 lignes de données)**
Given un CSV avec uniquement des lignes `#` et un header mais 0 lignes de données, When `POST /api/sleep/import`, Then `200`, `{"inserted": 0, "skipped": 0}`.

**TA-15 — CSV entièrement `#` (pas de header)**
Given un CSV composé uniquement de lignes `#`, When `POST /api/sleep/import`, Then `200`, `{"inserted": 0, "skipped": 0}` — pas d'exception.

**TA-16 — Encodage non-UTF-8**
Given un fichier binaire non-UTF-8 envoyé comme CSV, When `POST /api/sleep/import`, Then `422` avec `detail: "invalid_csv_encoding"`.

**TA-17 — Sécurité : filename avec path traversal**
Given un client authentifié envoyant un CSV valide avec `Content-Disposition: form-data; name="file"; filename="../../../etc/passwd"`, When `POST /api/sleep/import`, Then `200` (import traite le contenu, ignore le filename) et aucun accès filesystem sur le chemin du filename.

**TA-18 — Isolation user_id**
Given deux users authentifiés (`user_A` et `user_B`) important le même CSV sleep, When chacun fait `POST /api/sleep/import`, Then chaque user a ses propres `SleepSession` en DB (`user_id` distinct), et les compteurs `inserted` sont indépendants.

## Dépendances

| Dépendance | Nature | Statut |
|---|---|---|
| `server/db/models.py` (SleepSession, SleepStage, HeartRateHourly, StepsHourly, ExerciseSession) | ORM models cibles | Existant |
| `server/security/auth.get_current_user` | Auth JWT cookie | Existant |
| `server/security/rate_limit._api_post_cap`, `_user_id_key`, `limiter` | Rate-limit | Existant |
| `server/database.get_session` | Session DB | Existant |
| `server/logging_config.get_logger` | Structlog | Existant |
| `fastapi.UploadFile`, `fastapi.File` | Multipart | FastAPI stdlib |
| `io.StringIO`, `csv.DictReader` | Parsing CSV | Python stdlib |
| Alembic migration | Schema DB | Non requis (tables existantes) |

## RGPD

Les endpoints import sont **write-only** : aucune donnée de santé n'est retournée dans la réponse. La réponse contient uniquement des compteurs entiers (`inserted`, `skipped`). Les champs Art.9 sont chiffrés au moment de l'insertion via les ORM models (`EncryptedField`). Le fichier CSV n'est jamais stocké sur disque ni en mémoire au-delà du traitement de la requête. Conformité C1 (local-first) et C2 (chiffrement Art.9) garanties par le passage obligatoire par les ORM models.

## Suite naturelle

- `/tdd 2026-05-07-p4-backend-import` — écrire les 18 tests RED dans `tests/server/test_import_csv_multipart.py`
- `/impl 2026-05-07-p4-backend-import` — créer `server/services/csv_import.py` et ajouter les 4 endpoints dans les routers existants
- `/review` — audit pentester sur path traversal filename, injection CSV, taille fichier

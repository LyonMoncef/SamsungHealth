# SamsungHealth

## Problem

Samsung Health data is locked inside the mobile app with no open API. Visualizing sleep patterns over time requires manual CSV exports and external tools.

## Solution

An end-to-end pipeline that automatically syncs Samsung Health data via Android Health Connect, stores it in a database, and serves an interactive web dashboard with tabs for sleep, steps, heart rate, exercise, and trends.

## Features

| Feature | Files | Commit |
|---------|-------|--------|
| Sleep API + stage-aware calendar | `server/routers/sleep.py`, `static/app.js` | `8d5cfb0` |
| Steps API + bar chart | `server/routers/steps.py`, `static/app.js` | `242040a` |
| Heart rate API + range chart | `server/routers/heartrate.py`, `static/app.js` | `242040a` |
| Exercise API + card list | `server/routers/exercise.py`, `static/app.js` | `242040a` |
| Tabbed dashboard + trends | `static/index.html`, `static/app.js`, `static/style.css` | `242040a` |
| Android Health Connect sync | `android-app/` | `242040a` |
| Sample data generator | `scripts/generate_sample.py` | `242040a` |

## Architecture

```
Samsung Health → Health Connect ← Android App →HTTP→ FastAPI + SQLAlchemy →→ Postgres 16
                                                            ↓
                                                   Tabbed Web Dashboard
                                            (Sleep | Steps | HR | Exercise | Trends)
```

### Components

| Component | Tech | Status |
|-----------|------|--------|
| Backend API | Python, FastAPI, SQLAlchemy 2.x | Done |
| Database | Postgres 16 (Alembic-managed, UUID v7 PK) | Done (V2.1) |
| Web dashboard | HTML/CSS/JS (5 tabs) | Done |
| Android app | Kotlin, Jetpack Compose, Health Connect | Done |
| Sample data generator | Python (SQLAlchemy depuis V2.1.2) | Done |
| CSV import (Samsung Health export) | Python (SQLAlchemy + ON CONFLICT depuis V2.1.2) | Done |

## Setup

Prérequis : Docker (pour Postgres local).

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Démarrer Postgres + appliquer le schema
make db-up
make db-migrate
```

Variables d'environnement :
- `DATABASE_URL` — URL SQLAlchemy (défaut : `postgresql+psycopg://samsung:samsung@localhost:5432/samsunghealth`)
- `APP_ENV` — `dev` (ConsoleRenderer humain) ou `prod` (JSON stdout). Défaut : `prod`.
- `LOG_LEVEL` — `DEBUG | INFO | WARNING | ERROR | CRITICAL`. Défaut : `INFO`.

## Auth setup (V2.3)

Multi-utilisateur avec JWT HS256 + argon2id password hashing. Toutes les routes santé (`/api/*`) exigent `Authorization: Bearer <access_token>`.

Variables d'environnement requises :

```bash
# Génère un secret 256-bit base64 :
python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
SAMSUNGHEALTH_JWT_SECRET=<256-bit-base64-secret>

# Optionnel — secret précédent (decode-only) pour rotation sans logout global.
SAMSUNGHEALTH_JWT_SECRET_PREVIOUS=

# Token statique pour gating de POST /auth/register (header X-Registration-Token).
# Sans ce token l'endpoint répond 403 (registration_disabled).
SAMSUNGHEALTH_REGISTRATION_TOKEN=<32-chars-random>
```

### Endpoints

```
POST /auth/register   headers: X-Registration-Token, body: {email, password}
                      201 → {id, email}    409 → email_already_exists    403 → registration_disabled
POST /auth/login      body: {email, password}
                      200 → {access_token, refresh_token, token_type, expires_in}
                      401 → invalid_credentials  (identique sur user inconnu OU password faux)
POST /auth/refresh    body: {refresh_token}
                      200 → {access_token, refresh_token, ...}  (NOUVEAU refresh, ancien révoqué)
                      401 → invalid_refresh
POST /auth/logout     Authorization: Bearer <access>, body: {refresh_token}
                      204 (idempotent)
```

### Post-migration legacy user

Lors de la première migration alembic vers V2.3, si la table `users` est vide ET les tables santé contiennent déjà des données, un utilisateur `legacy@samsunghealth.local` (`is_active=false`, password aléatoire impossible-to-login) est auto-créé pour absorber les rows existantes via `user_id` FK. Action admin post-migration : créer un vrai user via `/auth/register` puis migrer manuellement les rows si désiré.

## Logs

Pipeline de logs structurés via `structlog` (V2.0.5).

- **Format prod** : 1 ligne JSON par event sur stdout, capture/rotation déléguée au runtime (docker compose, systemd).
- **Format dev** : `ConsoleRenderer` lisible humain (couleurs).
- **Champs standards** : `timestamp` (ISO8601 UTC), `level`, `logger` (scope), `event`, `request_id`, `user_id`, `route`, `latency_ms`.
- **Corrélation** : chaque request HTTP reçoit un `X-Request-ID` (généré ou propagé depuis le client) bindé via `contextvars` à tous les logs émis pendant son traitement. Le middleware émet `event: "request.complete"` à la fin avec `latency_ms` et `route` (template FastAPI).

Exemple ligne :
```json
{"event":"request.complete","logger":"server.middleware.request_context","route":"/api/sleep","method":"GET","status":200,"latency_ms":12.345,"request_id":"a1b2…","user_id":null,"level":"info","timestamp":"2026-04-26T10:00:00.000Z"}
```

## Usage

```bash
# Lancer le serveur
make dev

# Ouvrir http://localhost:8001 dans le navigateur

# Reset complet de la DB (DESTRUCTIVE — drop + recreate)
make db-reset
```

⚠️ **Migration depuis V1 (SQLite)** : V2.1 supprime SQLite. Si tu as un `health.db` legacy, sauvegarde-le séparément ; les CSV Samsung peuvent être ré-importés via le pipeline (refonte SQLAlchemy en cours via spec V2.1.2 — attendre avant ré-import).

## Related Projects

- [agent-schemas](https://github.com/LyonMoncef/agent-schemas) — Shared schema definitions
- [DocAgent](https://github.com/LyonMoncef/DocAgent) — Config query/edit agent
- [WidgetGenerator](https://github.com/LyonMoncef/WidgetGenerator) — JSON-to-Rainmeter generator

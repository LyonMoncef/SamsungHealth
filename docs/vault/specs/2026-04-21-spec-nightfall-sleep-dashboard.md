---
type: spec
title: "Spec — Nightfall Sleep Dashboard"
created: 2026-04-21
tags: [samsunghealth, spec, frontend, sleep, dashboard]
status: delivered
branch: feat/nightfall-dashboard
related_issues: []
implements:
  - file: static/dashboard.js
  - file: static/dashboard.css
  - file: static/index.html
  - file: static/api.js
tested_by: []
---

# Spec — Nightfall Sleep Dashboard

**Branche cible :** `feat/nightfall-dashboard` depuis `dev`
**Statut :** ready

---

## 1. Contexte & Motivation

Le dashboard actuel (`static/`) est un tableau basique (grille 24h × jours, 5 onglets). Le handoff Nightfall est une refonte visuelle complète. L'ancienne UI est mise de côté — Nightfall la remplace entièrement. Les autres sections (Steps, HR, Exercise) seront construites progressivement avec le même standard visuel dans de futures itérations.

Les données réelles sont déjà en DB (sync Android via Health Connect, Phase 3). Il n'y a pas d'API à créer — uniquement adapter le frontend au format attendu.

---

## 2. Goals (cette itération)

- [ ] Remplacer `static/index.html`, `static/style.css`, `static/app.js` par le design Nightfall complet
- [ ] Brancher sur `GET /api/sleep?include_stages=true` (30 dernières nuits)
- [ ] Adapter le format API → shape `window.SleepData` attendue par `dashboard.js`
- [ ] Steps et HR alimentés depuis l'API si disponibles, sinon 0 (pas bloquant)
- [ ] État vide géré proprement (DB sans données → message "Lancez une sync")

## 3. Non-Goals

- Refonte Steps / HR / Exercise / Trends (futures itérations, même standard Nightfall)
- Ajout de nouvelles données en DB (déjà fait)
- Responsive mobile (desktop-first)
- L'ancienne UI n'est pas supprimée manuellement — elle sera retirée progressivement

---

## 4. Architecture

### Flux de données

```
GET /api/sleep?from=YYYY-MM-DD&to=YYYY-MM-DD&include_stages=true
GET /api/steps?from=&to=
GET /api/heartrate?from=&to=
  → adapter() → window.SleepData
  → dashboard.js (inchangé — copié tel quel du handoff)
```

### Shape attendue par `dashboard.js`

```js
window.SleepData = {
  sessions: [{
    id,
    sleep_start: Date,
    sleep_end: Date,
    stages: [{ stage_type, stage_start: Date, stage_end: Date }],
    totals: { light, deep, rem, awake },  // en ms
    duration_ms, duration_hours,
    efficiency, score,
    date_key,                              // "YYYY-MM-DD"
  }],
  steps:  { "YYYY-MM-DD": number },       // total journalier
  hr:     { "YYYY-MM-DD": number },       // resting BPM nocturne
  summary: { avgDur, avgScore, avgRem, avgDeep, avgLight, avgAwake,
             regularity, bedtimeStdDev, debt, nights,
             avgRestingHR, avgSteps }
}
```

### Ce que l'API fournit

- `GET /api/sleep?include_stages=true` → `[{ sleep_start, sleep_end, stages[] }]` (ISO strings)
- `GET /api/steps` → `[{ date, hour, step_count }]` → sommer par jour
- `GET /api/heartrate` → `[{ date, hour, avg_bpm }]` → avg heures 0–6 comme resting HR

### Stratégie d'adaptation (`static/api.js`)

1. Fetch les 3 endpoints (30 dernières nuits)
2. Construire les objets `Date` depuis les ISO strings
3. Calculer `totals`, `duration_ms`, `efficiency`, `score` (même formule que `data.js`)
4. Agréger steps et HR par jour
5. Calculer `summary`
6. Si 0 sessions → injecter l'écran vide dans `#app` et s'arrêter
7. Sinon exposer `window.SleepData` et appeler `render()`

---

## 5. Fichiers touchés

| Fichier | Action |
|---------|--------|
| `static/index.html` | Remplacé — structure Nightfall, `api.js` à la place de `data.js` |
| `static/style.css` | Remplacé par `dashboard.css` du handoff |
| `static/app.js` | Remplacé par `dashboard.js` du handoff |
| `static/api.js` | **Nouveau** — fetch + adapter + empty state + appel render |

L'ancienne UI (`app.js`, `style.css`) n'est pas archivée — elle sera retirée en douceur dans les futures itérations quand les nouvelles sections Nightfall la remplaceront.

---

## 6. Plan d'implémentation

1. [ ] Créer la branche `feat/nightfall-dashboard` depuis `dev`
2. [ ] Tests d'abord : `tests/test_sleep_api_shape.py` — vérifier shape `GET /api/sleep?include_stages=true`
3. [ ] Extraire `dashboard.css` et `dashboard.js` du zip → `static/`
4. [ ] Écrire `static/api.js` — fetch + adapter + empty state
5. [ ] Réécrire `static/index.html` — structure Nightfall
6. [ ] `make dev` — tester avec données réelles + vérifier état vide
7. [ ] `/review` → `/pr` → label `tested` → merge vers `dev`

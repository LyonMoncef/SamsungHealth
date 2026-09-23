---
title: "Usage Stats Collector — collecte locale des données de bien-être numérique"
slug: 2026-05-09-usage-stats-collector
status: draft
created: 2026-05-09
implements: []
tested_by: []
---

# Spec — Usage Stats Collector

## Vision

Étendre le local-first de Nightfall au-delà du sommeil Samsung Health : capturer **les données de bien-être numérique** (temps d'app, lancements, app foreground vs background) directement depuis l'API Android `UsageStatsManager`, en local, sans dépendance externe.

À terme, **fusionner** sommeil + usage numérique dans une vue unifiée pour repérer des corrélations (ex: "les nuits où j'ai utilisé YouTube après 23h sont les plus dégradées"). Hors scope de cette spec — c'est l'objectif final qui justifie la collecte.

## Pourquoi cette spec ?

Android UsageStats expose un historique limité par design :
- ~10 jours en granularité quotidienne
- ~6 mois en monthly
- ~1 bucket yearly courant (~3 mois)

Les buckets archivés sont **inaccessibles via dumpsys** (cf. snapshot 2026-05-09 — fichier `1738850483925` du 06/02/2025 protégé par UID system). Solution : **agréger soi-même côté app**. Sur 6-12 mois de collecte, on dispose d'un historique plus profond que le device.

Cohérent avec la VISION de Nightfall :
- **C1 — Local-first absolu** ✅ aucune transmission réseau
- **C2 — RGPD niveau maximum** ✅ chiffré au repos via SQLCipher (réutilisé Phase A sleep)
- **C3 — Sécurité intégrée** ✅ pentester audit à étendre
- **No LLM on health data** ✅ traitement algorithmique uniquement

## Décisions techniques

### Source de données
- **API officielle** `android.app.usage.UsageStatsManager`
- Permission requise : `android.permission.PACKAGE_USAGE_STATS` (signature|privileged|appop)
- Activée manuellement par l'utilisateur via Settings → Apps → Usage Access → toggle Nightfall
- Pas de root, pas d'OEM unlock, pas de Knox tripped

### Données capturées (par jour, par package)
- `totalTimeInForeground` — temps total au premier plan
- `totalTimeVisible` — temps visible (Android 10+)
- `totalTimeForegroundServiceUsed` — temps en FGS
- `lastTimeUsed` — dernière utilisation
- `appLaunchCount` — nombre de lancements (Android 12+)

Plus, en option (Phase B_us) : `UsageEvents` stream pour reconstruire des sessions précises (ACTIVITY_RESUMED, ACTIVITY_PAUSED, etc.).

### Stockage local
- Réutilise `NightfallDatabase` (Room + SQLCipher) déjà en place — pas de nouvelle DB
- Nouvelles tables :
  - `usage_daily` — un row par (date, package), agrégat journalier
  - `usage_events` (Phase B_us) — events bruts pour analyse fine
- Entities en `data/local/entity/usage/`
- DAOs en `data/local/dao/`

### Collecte
- **WorkManager** quotidien (`PeriodicWorkRequest`, 1 jour, contraintes : batterie OK)
- À chaque exécution : query `UsageStatsManager.queryUsageStats(INTERVAL_DAILY, lastDay, now)` → upsert dans Room
- Idempotent : `OnConflictStrategy.REPLACE` sur (date, package) — le total recalculé écrase l'ancien (Android peut affiner les compteurs après-coup)
- Pas de Foreground Service — Android tue sans pénalité, on rattrape au prochain run

### Permission UX
- À la 1re ouverture de l'écran Bien-être Numérique :
  - Vérif `AppOpsManager.OPSTR_GET_USAGE_STATS == MODE_ALLOWED`
  - Si non : écran d'onboarding expliquant la démarche, bouton "Activer l'accès" qui lance `Settings.ACTION_USAGE_ACCESS_SETTINGS`
- Pas d'accès = pas de collecte, pas d'erreur (graceful degradation)

## Phases

### Phase A_us — Foundation (cette PR)
- Spec + Room entities + DAOs
- `UsageStatsPermissionHelper` — check + intent vers Settings
- `LocalUsageStatsService` — wrapper async autour de `UsageStatsManager` qui écrit dans Room
- Tests Robolectric pour DAO + helper + service (avec UsageStatsManager mocké)
- **Pas d'UI, pas de WorkManager** — juste l'infra

### Phase B_us — Collecte automatique
- `WorkManager` daily worker qui appelle `LocalUsageStatsService.collectDailyStats()`
- Bootstrap : à la 1re ouverture, backfill avec `INTERVAL_YEARLY` (jusqu'à ce que l'API expose)
- Tests sur le worker (in-memory Room)

### Phase C_us — UI Bien-être Numérique
- Écran dédié dans Nightfall (`UsageScreen`) — barres temps/app/jour
- Top 10 par jour / par semaine
- Graphiques d'évolution (drift comme Timeline sleep)

### Phase D_us — Fusion sommeil + usage
- Vue corrélée : pour chaque nuit, les 3-4h précédant le coucher → top apps utilisées
- Score "hygiène numérique" pré-sommeil
- À spec à part — gros chantier UX

## Livrables Phase A_us

- [ ] `data/local/entity/usage/UsageDailyEntity.kt`
- [ ] `data/local/dao/UsageStatsDao.kt`
- [ ] Migration NightfallDatabase v1 → v2 (ajout des nouvelles tables)
- [ ] `data/local/usage/UsageStatsPermissionHelper.kt`
- [ ] `data/local/usage/LocalUsageStatsService.kt`
- [ ] Manifest : `<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions" />`
- [ ] Tests Robolectric in-memory

## Tests d'acceptation

1. **Permission gating** — given une app sans `PACKAGE_USAGE_STATS` accordé, when on appelle `UsageStatsPermissionHelper.hasPermission()`, then retourne false. Quand on appelle `intentToGrantPermission()`, ça produit un `Intent` avec action `Settings.ACTION_USAGE_ACCESS_SETTINGS`.

2. **Collecte daily idempotente** — given un mock `UsageStatsManager` retournant 3 packages avec leur `totalTimeInForeground`, when `collectDailyStats(day=2026-04-20)` est appelé, then 3 rows sont écrits dans Room ; un 2e appel avec valeurs mises à jour les écrase (REPLACE).

3. **Migration Room v1→v2** — given une DB v1 (sleep + heart_rate + steps + exercise tables), when ouverture en v2, then les nouvelles tables `usage_daily` sont créées sans perte des données existantes.

4. **Pas d'UI cassée** — given Phase A_us en place, when on lance toutes les UI existantes (Sleep, Timeline, Hypnogram, Import), then aucune régression visible. La feature usage_stats est dormante sans wiring UI.

## Suite naturelle

Après Phase A_us validée et mergée vers main :
- Phase B_us : worker
- Phase C_us : UI
- Phase D_us : fusion sleep+usage

## Risques et mitigations

- **L'utilisateur n'active pas la permission** → graceful degradation, écran d'onboarding clair, on n'insiste pas
- **API limit / quota Android** → `queryUsageStats` peut renvoyer vide en backfill profond ; on accepte la limite native, on collecte daily en avance pour densifier l'historique
- **Migration Room v2 brick la DB existante** → tests `MigrationTestHelper` obligatoires avant merge en main, branche longue durée pour itérer sans pression
- **Knox / GrapheneOS cas particuliers** → tests sur device réel à chaque release, fallback empty list si API throw

## Lien avec autres specs

- Hérite directement de `2026-05-09-local-first-migration.md` (Phase A pour Room+SQLCipher, Phase B pour Service architecture, Phase C pour repository pattern)
- Sera référencé par futur `2026-XX-sleep-usage-correlation.md`

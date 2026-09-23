# Roadmap — Virage on-device (2026-09-23)

> **Ce document remplace la roadmap pré-virage** (Phases 1-6 client-serveur, désormais
> caduques). L'ancienne roadmap et tout le travail réalisé restent dans l'historique git
> et `HISTORY.md`. Décision d'architecture : voir `NOTES.md` ADR-4.

## Cap

Nightfall devient une **app Android on-device pure** (modèle [darkhour-android](https://github.com/Aozora7/darkhour-android), MIT — inspiration, pas copie). Plus de backend. Données via **Health Connect**. Analyse circadienne dans un module `core/` Kotlin pur. Objectif produit inchangé : rendre visible le rythme (Non-24) que les chiffres bruts masquaient.

**Principes directeurs :**
- **On-device absolu** — les données santé ne quittent jamais l'appareil (C1/C2 gratuits, plus de serveur à sécuriser).
- **Algos = science publiée réimplémentée**, jamais copiée. darkhour = *oracle de validation* exécutable (`core/` JVM-pur), pas une source à plagier.
- **Ne plus développer à l'aveugle** — chaque algo est prototypé et validé en notebook Python (Darkhour + libs de référence comme oracles) avant port Kotlin. Voir Phase 2.
- **Lisibilité > efficacité** dans tout code produit (l'utilisateur doit pouvoir relire/s'approprier).

---

## Phase 0 — Nettoyage & bascule (EN COURS)

- [x] Archiver données santé + APK hors repo → `S2:/data3/Projets/SamsungHealth` (md5 vérifié)
- [x] `.gitignore` durci (`*.apk`, `desktop-import/`)
- [x] Nettoyage branches : `main` + `dev` seulement (local + origin) ; travail préservé (PR mergées + tags `archive/*`)
- [x] Tag `checkpoint-pre-pivot-2026-09-23`
- [x] Scan secrets historique (gitleaks, 350 commits + refs PR) → **0 fuite**, pas de filter-repo ni rotation
- [x] Retrait des hooks cartographer (`pre-commit`/`post-commit`) — vault obsolète
- [x] Retirer de l'arbre de travail : `server/`, `alembic/`, `static/`, `docker*`, `.github/workflows/deploy-*` (historique conservé)
- [x] **Décision structure** : projet Gradle promu à la racine (`app/` + futur `core/`), build validé
- [x] Toolchain Android sur S1 (JDK 21 + SDK, compilation et tests unitaires en local)
- [ ] Venv Python minimal pour le notebook (Phase 2)

## Phase 1 — Fondations données on-device

- [x] 1.0 Purge du code lié au serveur (auth, Retrofit, flavor webview, ping backend)
- [ ] Health Connect : déclarer `READ_HEALTH_DATA_HISTORY`, flux de permissions
- [ ] Lecture sommeil + pagination de **l'historique complet** (chunks), dédup multi-source
- [ ] **Contrat de données canonique gelé** (`SleepRecord` + « activity epoch ») — partagé notebook ↔ Kotlin
- [ ] Valider en conditions réelles la profondeur d'historique effectivement poussée par Samsung dans HC
- [ ] Export d'un dataset (CSV/Parquet) au contrat gelé pour le harnais

## Phase 2 — Harnais de validation (notebook)

- [ ] Notebook Python : **algos dans les cellules**, plomberie dans `helpers.py`
- [ ] Oracles : darkhour JVM (`core/` pur) sur données identiques + libs réf (`nparACT`/`pyActigraphy`, `astropy.LombScargle`, `filterpy`)
- [ ] Deux modes : données synthétiques à vérité connue (justesse) + parité darkhour (données réelles)
- [ ] Le notebook **émet des golden fixtures** → tests du `core/` Kotlin (garantit port == notebook validé)
- [ ] Plotly = validation logique data→géométrie (≠ design UI)
- [ ] Emplacement : `research/` ou `notebooks/` versionné, fixtures dans `core/src/test/resources/`

## Phase 3 — Moteur d'analyse (`core/` Kotlin)

Du plus simple au plus dur, chaque brique validée en Phase 2 avant port :
- [ ] **NPCRA** (IS/IV/RA/M10/L5) sur pas Health Connect — *différenciateur vs darkhour* (qui ne fait que le sommeil)
- [ ] **Périodogramme** (Lomb-Scargle / Sokolove-Bushell) — détection de période
- [ ] **τ + Kalman/RTS** — estimation de la période circadienne et sa dérive (darkhour = oracle)

## Phase 4 — UI & identité Nightfall

- [ ] Actogramme double-plot + vues sommeil / circadien (best practices darkhour, **identité visuelle Nightfall/DataSaillance**, pas de copie)
- [ ] Couche produit propre : croisement **sommeil × usage écran × lieux** (l'idée originale de Nightfall, absente de darkhour) — on-device (`UsageStatsManager`, GPS)

## Phase 5 — Publication & forge

- [ ] Bascule forge **GitHub → Gitea**
- [ ] **Clone à historique vierge** pour publication à V1 (le repo historique reste privé, figé en archive)
- [ ] Profil de style de code extrait du corpus de projets Python de l'utilisateur → mémoire

---

## Questions ouvertes

- Structure `core/`+`app/` racine vs `android-app/` — à trancher en Phase 0.
- NPCRA sur pas HC = proxy d'activité plus grossier que l'actigraphie classique (à assumer dans l'interprétation).
- Croisement circadien × GPS : piste de recherche récente (accéléromètre/GPS smartphone) à explorer en Phase 4.

_Dernière mise à jour : 2026-09-23_

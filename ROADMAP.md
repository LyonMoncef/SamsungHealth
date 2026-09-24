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
- [x] Venv Python minimal pour le notebook (`notebooks/.venv`, voir `notebooks/README.md`)

## Phase 1 — Fondations données on-device (TERMINÉE, validée sur téléphone le 2026-09-24)

- [x] 1.0 Purge du code lié au serveur (auth, Retrofit, flavor webview, ping backend)
- [x] 1.2 Health Connect : `READ_HEALTH_DATA_HISTORY`, flux de permissions, écran de justification, écran Health Connect
- [x] 1.2 Lecture sommeil + pas de **l'historique complet** (pagination), dédup par identifiant ; écrans existants branchés, import CSV supprimé. *La dédup multi-source devient un calcul (Phase 2/3).*
- [x] 1.1 **Contrat de données** dans le module `core/` : `SleepRecord` (brut) + `HourlySteps` (v2, totaux horaires Health Connect) ; export CSV
- [x] **TA-13** : 1153 sessions depuis le 2024-07-09 05:52 local (~26 mois), historique complet. Écart avec darkhour (1146, « 8 juillet ») expliqué : 6 paires de sessions Samsung qui se chevauchent (darkhour les fusionne) + 1 nuit après son relevé ; darkhour range le sommeil de 05:52 dans la nuit du 8. 18 790 heures de pas (≈ 97 % des heures, 4 sources)
- [x] 1.3 Export ZIP (3 CSV + manifeste) depuis Profil, pour le harnais
- [x] **TA-14** : archive exportée, les 3 CSV se chargent tels quels dans pandas, comptes identiques au manifeste

## Phase 2 — Harnais de validation (notebook)

- [x] **Premier calcul validé : la dédup des sessions qui se chevauchent** (`notebooks/01_dedup_chevauchements.ipynb`). 6 paires dont 5 doublons ≥ 80 % (une chaîne de 3 le 26/09/2025) → **1147 épisodes d'analyse**, 1148 sessions affichées ; **parité darkhour : 1146** sur les données de son relevé du 23/09
- [x] **Règle Nightfall (analyse)** : fusion aussi des sessions **bout à bout** (écart nul), signature des corrections manuelles après une panne de montre (12 des 13 cas : montre avec stades + complément sans stades) → **1134 épisodes**. Écart volontaire avec darkhour ; l'affichage garde la règle des doublons ≥ 80 %

- [x] Notebook Python : **algos dans les cellules**, plomberie dans `helpers.py` ; `nbstripout` retire les sorties avant commit (C1)
- [ ] Oracles : darkhour JVM (`core/` pur) sur données identiques + libs réf (`nparACT`/`pyActigraphy`, `astropy.LombScargle`, `filterpy`)
- [ ] Deux modes : données synthétiques à vérité connue (justesse) + parité darkhour (données réelles)
- [x] Le notebook **émet des golden fixtures** → tests du `core/` Kotlin (garantit port == notebook validé) : 9 cas inventés pour la dédup, au format du contrat d'export
- [ ] Plotly = validation logique data→géométrie (≠ design UI)
- [x] Emplacement : `notebooks/` versionné ; fixtures à venir dans `core/src/test/resources/`

## Phase 3 — Moteur d'analyse (`core/` Kotlin)

Du plus simple au plus dur, chaque brique validée en Phase 2 avant port :
- [x] **Dédup des sessions** (`core/.../dedup/SleepDeduplication.kt`) : épisodes d'analyse (règles darkhour et Nightfall) + sessions affichées ; 9 fixtures × 3 vues vertes, et identique au notebook sur l'export réel (1134 épisodes, 1148 sessions affichées, mêmes identifiants)
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

_Dernière mise à jour : 2026-09-24_

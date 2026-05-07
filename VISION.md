# Nightfall — Vision Produit

> Document vivant. Mis à jour après chaque conversation narrative.
> Quand une spec ne correspond pas à cette vision, on discute avant de coder.

---

## Principe fondateur

C'est en **construisant** les visualisations de ses données de sommeil que l'utilisateur a découvert et mis un nom sur un trouble qui durait depuis 15 ans — le Non-24 (syndrome hypernychthéméral). Aucun médecin, aucune polysomnographie n'avait abouti à ce résultat. Le dashboard l'a rendu visible.

Nightfall existe parce que **les données de santé personnelles, visualisées proprement, peuvent révéler ce que la médecine standard rate**. Ce n'est pas un side-project — c'est un outil de self-diagnostic pour une pathologie rare, construite par quelqu'un qui en souffre.

Angle : on ne demande pas à l'utilisateur d'interpréter des chiffres bruts — on lui montre des **patterns** qu'il n'aurait jamais vus autrement (drift circadien, compressibilité du sommeil, dette accumulée).

---

## Ce que ce projet n'est pas

- Pas un tracker de sommeil comme les autres (Samsung Health app, Oura, Whoop) — on visualise ce qu'eux ne montrent pas
- Pas une app de conseils ("dormez 8h") — aucun jugement, seulement de la saillance
- Pas un cloud service — les données de santé restent sur l'infrastructure de l'utilisateur
- Pas un produit grand public — usages médicaux personnels, conformité RGPD santé niveau max
- Pas un LLM wrapper sur des données de santé

---

## Vocabulaire

| Terme | Définition |
|-------|-----------|
| **Drift** | Décalage progressif des heures de coucher/réveil. Dans le Non-24, une journée dure plus que 24h — le drift s'accumule, le cadran tourne. |
| **Dette de sommeil** | Écart cumulé entre le sommeil réalisé et le besoin estimé. |
| **Régularité** | Variabilité de la durée de sommeil. Un sommeil sain = bout de bois (fixe). Un sommeil pathologique = élastique (compressible et extensible selon la dette). |
| **Radial clock** | Vue polaire 24h où chaque nuit est un arc. Visualise le drift comme une rotation du cycle sur le cadran. |
| **Stacked timeline** | Vue linéaire infinie, une ligne par jour, colonnes = heures. Permet de voir le drift et les patterns semaine/weekend en un coup d'œil. |
| **Infinite scroll** | Chaque viz peut s'ouvrir en vue dédiée avec toutes les données depuis le début, sans fenêtre fixe de 30 jours. |
| **Wake-anchored** | Référentiel circadien personnel : T+0 = réveil, pas minuit. Rend les patterns comparables même quand le calendrier dérive. |

---

## Contraintes hard

### C1 — Local-first absolu

Les données Samsung Health sont des données médicales (Art.9 RGPD). Elles ne quittent jamais l'infrastructure contrôlée par l'utilisateur. Aucun envoi vers des serveurs tiers, aucun traitement cloud sur les données brutes.

> *"Les données et leur traitement se feront toujours localement."* — session 2026-04-21

Conséquences :
- Chiffrement AES-256-GCM applicatif sur tous les champs Art.9 (at-rest)
- TLS 1.3 obligatoire (Caddy) pour le transit
- Self-hosted uniquement (VPS personnel, pas PaaS)
- Architecture 12-factor pour que le switch cloud HDS futur soit une question d'env vars

### C2 — RGPD santé niveau maximum

Audit 12 mois, endpoints export/erase/audit-log, consentement traçable. Non-négociable même en usage mono-utilisateur.

### C3 — Sécurité intégrée au cycle de dev

Pentester agent invoqué à chaque spec et chaque PR. Verdict bloquant si finding ≥ HIGH. Pas une étape optionnelle, une porte de sortie obligatoire.

---

## Design system — DataSaillance

**Règle fondamentale** : Nightfall v2 utilise la charte graphique DataSaillance issue de l'identité visuelle officielle. Cohérence avec l'écosystème (DataSaillance, HarnessGame).

### Référence

- **Palette et logos (source de vérité)** : `../Vectorizer/IdentiteVisuelle/`
- **Méthodologie CSS** : `../OpenDesign/` (structure global.css, organisation des fichiers) — pas les tokens couleur
- **Polices** : `../Vectorizer/IdentiteVisuelle/Polices/` (Playfair Display) + Cairo via Google Fonts

### Tokens (source of truth — extraits des logos IdentiteVisuelle)

**Dark mode** :
```css
--bg: #191e22           /* fond principal dark */
--bg-panel: #232e32     /* surface / cards */
--text: #e8eff2
--text-muted: #7a9aaa
--accent-teal: #0e9eb0  /* accent primaire */
--accent-amber: #d37c04 /* accent secondaire / CTA */
--accent-cyan: #3be5e7  /* accent tertiaire / highlights */
--border: #2e3d44
```

**Light mode** :
```css
--bg: #ffffff
--bg-panel: #f4f8fa
--text: #191e22
--text-muted: #81868b
--accent-teal: #0e9eb0
--accent-amber: #d37c04
--accent-cyan: #3be5e7
--border: #d0dde3
```

### Règles d'application

- **Mode light ET dark obligatoires** — toggle utilisateur + respect du `prefers-color-scheme` OS
- **Accent teal** (`#0e9eb0`) pour les éléments primaires (liens, focus, sélection)
- **Accent amber** (`#d37c04`) pour les CTA et actions principales
- **Accent cyan** (`#3be5e7`) pour les highlights et visualisations de données
- **Police** : Cairo (variable, 400/500/600/700) + fallback système
- **Pas de gradients décoratifs** — surfaces plates, hiérarchie par poids et taille
- **Pas de `#6366f1`** (indigo Tailwind) — anti-slop explicite
- **Pas de glows/halos** — box-shadow décoratif interdit
- Tracking uppercase : `letter-spacing: 0.06em` minimum — obligatoire

### Ce que Nightfall v1 avait et qu'on abandonne

- Fond noir pur `#000` / contrastes extrêmes → remplacé par `#191e22`
- Halos lumineux et glows décoratifs → supprimés
- Couleurs aléatoires non issues de la charte → remplacées par tokens IdentiteVisuelle
- Originalité visuelle du nom "Nightfall" → conservé comme nom produit, pas comme direction esthétique

---

## Architecture cible

```
DEV (PC local) :
  docker-compose up → Caddy (443) + FastAPI (8001) + Postgres + Mailpit + Adminer
  Android emulator → APK WebView → charge https://10.0.2.2/static/
  Browser localhost → dashboard web

PROD (VPS self-hosted OVH) :
  docker-compose.prod.yml → Caddy (443 Let's Encrypt) + FastAPI + Postgres
  Android release → Compose natif (auth + dashboard Canvas + import SAF + Health Connect)
  Deux environnements : sh-dev.datasaillance.fr (auto-deploy main) / sh-prod.datasaillance.fr (manuel)
```

**Dual UI** : les specs sont source de vérité unique. WebView en dev pour itérer vite dans le browser, Compose Canvas natif en prod pour le rendu final.

**Import data** : Storage Access Framework Android → parser Samsung Health ZIP/CSV → POST backend. Health Connect pour la sync continue.

---

## Visualisations cœur

### Stacked timeline (infinie)

- Axe Y = 24h, Axe X = toutes les nuits depuis le début (1200+ nuits)
- Ouvrable depuis n'importe quelle vue en full-screen infinite scroll
- Weekends colorés différemment pour repérer les patterns hebdo
- Plage de sommeil "idéale" en fond pour visualiser les écarts
- Sticky headers colonnes-heures au scroll

### Radial clock

- Vue polaire 24h, un arc par nuit = heure coucher → heure réveil
- Visualise le drift comme un arc qui tourne progressivement
- Animations prev/next pour naviguer nuit par nuit
- Fenêtre temporelle dynamique (pas hardcodée 12h-12h — le drift casse une fenêtre fixe)

### Régularité du sommeil

- Métaphore bout de bois vs élastique traduite en dataviz
- Indicateur de compressibilité (nuit courte) et extensibilité (rattrapage de dette)
- Nombre de cycles (alternances phases) comme signal complémentaire

### Hypnogramme

- Stages par nuit (léger / profond / REM / éveillé)
- Navigation prev/next entre nuits
- KPIs en section dédiée (durée par stage, efficacité)

---

## Positionnement dans l'écosystème DataSaillance

SamsungHealth / Nightfall n'est pas un projet isolé. Il s'inscrit dans un écosystème :

- **DataSaillance** = agence / plateforme webapp (patterns auth, CI/CD, design system)
- **Nightfall** = produit santé personnel, adopte les standards DataSaillance
- **HarnessGame** = monitoring des agents — vision future d'une webapp cross-projet
- **WorkflowViz** = visualisation du process de dev — couche transverse

La migration vers une webapp unifiée (Nightfall dans DataSaillance) est une option backlog, pas une cible immédiate.

---

## Phases de développement

| Phase | Contenu | Statut |
|-------|---------|--------|
| **0 — Infra** | Postgres, Alembic, Caddy, structlog, docker-compose | ✅ |
| **1 — Auth** | Email/pwd, Google OAuth, reset, JWT, rate limiting | ✅ |
| **2 — Data layer** | Routers async, chiffrement AES-256-GCM Art.9, multi-user | ✅ |
| **3 — RGPD** | export/erase/audit-log, consentement | ✅ |
| **6 — CI/CD** | deploy-dev + deploy-prod, Caddy HTTPS, smoke tests | ✅ |
| **4 — Android** | Compose shell (auth/import SAF/settings) + WebView dashboard | En attente |
| **5 — Compose Canvas** | Dashboard natif (hypnogramme, radial, timeline, cards) | En attente |
| **UI redesign** | Refonte complète Nightfall → tokens DataSaillance, light+dark | À intégrer dans Phase 4+5 |

---

## Questions ouvertes

- Fenêtre circadienne : algorithme de détection automatique de la "journée" sans hardcoder 12h-12h ?
- Multi-user : Nightfall reste mono-user perso ou s'ouvre à famille/proches ?
- Export médical : format standardisé (FHIR, JSON-LD) pour partage avec médecin ?
- Webapp unifiée : quand basculer Nightfall dans DataSaillance vs rester repo indépendant ?
- Phase B (Phoenix observabilité) : avant ou après Phase 4 Android ?

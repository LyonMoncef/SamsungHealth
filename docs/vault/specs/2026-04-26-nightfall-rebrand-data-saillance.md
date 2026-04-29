---
type: reference
title: "Nightfall — Rebrand Data Saillance (palette + typo + logos + dark/light)"
slug: 2026-04-26-nightfall-rebrand-data-saillance
status: ready
created: 2026-04-26
delivered: null
priority: high
related_plans:
  - 2026-04-23-plan-v2-refactor-master
related_specs:
  - nightfall-fullspectrum-design-brief
supersedes:
  - nightfall-fullspectrum-design-brief
consumed_by: []  # à peupler par chaque future spec frontend (web, Android shell, Compose Canvas)
external_refs:
  - repo: ~/MyPersonalProjects/Vectorizer
    description: "Atelier d'identité visuelle Data Saillance (source of truth logos, palette, typo)"
  - file: ~/MyPersonalProjects/Vectorizer/design-system/colors.md
    description: "Palette canonique"
  - file: ~/MyPersonalProjects/Vectorizer/design-system/typography.md
    description: "Système typo + type scale"
  - dir: ~/MyPersonalProjects/Vectorizer/IdentiteVisuelle/Logo/
    description: "5 variantes logo (DarkMode, WhiteMode, WhiteModeFondBleu, FondTransparent, Favicon)"
tags: [frontend, design-system, rebrand, nightfall, data-saillance, dark-mode, light-mode, reference]
---

# Nightfall — Rebrand Data Saillance

## Vision

**Data Saillance** est le nom de l'agence/structure qui produit l'outil. **Nightfall** reste le nom du produit (dashboard santé personnel). On adopte la charte graphique Data Saillance pour habiller Nightfall, avec **dark mode ET light mode** (le light mode n'existait pas dans Nightfall v1 du loom — uniquement dark).

Cette spec est une **référence design partagée** : elle ne livre pas de code, elle fige les décisions visuelles que toute future spec frontend (web `static/`, Android Compose shell, Compose Canvas natif viz) doit consommer via son frontmatter `related_specs:`.

Elle **supersedes** `nightfall-fullspectrum-design-brief.md` (status passé `ready` → `superseded`).

## Source of truth

Le repo séparé **`~/MyPersonalProjects/Vectorizer/`** est la source canonique des assets (SVG/PNG logos, fichiers polices). Cette spec en **copie les valeurs** (palette HEX, type scale, double-stroke specs) pour que SamsungHealth reste lisible/utilisable sans dépendre du chemin disque externe.

Quand Vectorizer évolue (nouvelle déclinaison logo, ajustement palette), un commit de sync explicite met à jour cette spec — on ne le synchronise PAS automatiquement.

## Palette canonique

### Light mode (Nightfall v2 — nouveau)

| Rôle | Token | HEX | RGB |
|------|-------|-----|-----|
| Fond principal | `--ds-bg-primary` | `#FAFAFA` | `rgb(250, 250, 250)` |
| Fond coloré (hero/avatar) | `--ds-bg-accent` | `#0E9EB0` | `rgb(14, 158, 176)` |
| Accent chaud | `--ds-accent-warm` | `#D37C04` | `rgb(211, 124, 4)` |
| Accent froid | `--ds-accent-cool` | `#07BCD3` | `rgb(7, 188, 211)` |
| Neutre / contour | `--ds-neutral` | `#828587` | `rgb(130, 133, 135)` |

### Dark mode (Nightfall v2 — préservé v1, palette Data Saillance étendue)

| Rôle | Token | HEX | RGB |
|------|-------|-----|-----|
| Fond principal (near-black) | `--ds-bg-primary` | `#191E22` | `rgb(25, 30, 34)` |
| Fond intra-formes (slate) | `--ds-bg-secondary` | `#232E32` | `rgb(35, 46, 50)` |
| Accent chaud | `--ds-accent-warm` | `#D37C04` (ou variantes halo plat ↓) | — |
| Accent froid | `--ds-accent-cool` | `#07BCD3` (ou variantes halo plat ↓) | — |

### Effet "halo plat" (dark mode uniquement, pour SVG vectoriel)

Pas de filtre blur SVG. Chaque trait coloré = **deux strokes superposés** (externe saturé + interne clair plus fin) :

| Trait | Stroke externe (largeur 20-40) | Stroke interne (largeur 16.5-22) |
|-------|--------------------------------|----------------------------------|
| Cyan | `#3BE5E7` width 20 | `#8DFFFF` width 16.5 |
| Orange | `#854808` width 40 | `#FCBF0E` width 22 |

Remplissage formes intérieures : `#232E32` (se fond dans `--ds-bg-primary` tout en restant distinct).

### Règles d'usage

- **Orange** : accent principal, points focaux (états actifs, CTA primaire, valeur clé KPI)
- **Cyan** : accent secondaire, traits structurants (lignes graph, axes, cadres)
- **Gris** (light mode uniquement) : neutres, contours secondaires
- Les couleurs de fond (`#FAFAFA`, `#191E22`, `#0E9EB0`) ne sont **jamais** utilisées comme couleur de trait

## Typographie canonique

### Système — 2 familles

| Rôle | Famille | Poids | Style | Usage |
|------|---------|-------|-------|-------|
| **Display / Wordmark / Titres H1-H2** | **Playfair Display** | 700 (Bold) | Roman | Nom de marque, hero, H1, H2 |
| **Body / Texte courant / H3-H4 / UI** | **Inter** | 400 / 500 / 600 | Roman | Paragraphes, signatures, captions, labels UI |

**Règle** : H1/H2 en serif (Playfair, contraste éditorial). H3+ et UI en sans-serif (Inter, lisibilité).

### Type scale (base 16 px, ratio 1.25)

| Rôle | Famille | Taille | Poids | Notes |
|------|---------|--------|-------|-------|
| Hero / Display 1 | Playfair Display | 56-72 px | 700 | Bannière, hero web |
| H1 / Titre | Playfair Display | 40 px | 700 | Titre de page |
| H2 / Sous-titre | Playfair Display | 32 px | 700 | Section principale |
| H3 / En-tête | Inter | 22 px | 600 | Sous-section |
| H4 / En-tête de section | Inter | 18 px | 600 | Sub-sous-section |
| Eyebrow / Label | Inter | 13 px | 500 | uppercase, letter-spacing 0.15em |
| Body | Inter | 16 px | 400 | Texte courant |
| Body small | Inter | 14 px | 400 | Texte secondaire |
| Citation | Playfair Display | 20-22 px | 400 | *italic* |
| Légende | Inter | 13 px | 400 | opacité 70%, caption/métadonnée |

### Sources fichiers polices

- `~/MyPersonalProjects/Vectorizer/IdentiteVisuelle/Polices/Playfair_Display/` (variable + static + OFL.txt)
- `~/MyPersonalProjects/Vectorizer/IdentiteVisuelle/Polices/Inter/` (cf. design-system/typography.md pour mapping Canva)

À la première impl frontend : copier les fichiers nécessaires dans `static/assets/fonts/` (web) ou `android-app/src/main/assets/fonts/` (Android), avec licence OFL préservée.

## Logos

5 variantes disponibles dans `~/MyPersonalProjects/Vectorizer/IdentiteVisuelle/Logo/` :

| Variante | Fichier source | Usage |
|----------|----------------|-------|
| Dark mode | `DarkMode/logo-dark_1260.svg` | Header sur fond sombre `#191E22` |
| White mode (fond blanc) | `WhiteMode/logo-white-fond-blanc_1260.svg` | Header sur fond clair `#FAFAFA` |
| White mode (fond bleu) | `WhiteModeFondBleu/logo-white-fond-bleu_1260.svg` | Avatar social, sticker, en-tête de carte |
| Fond transparent | `FondTransparent/logo-white-sans-fond_1260.svg` | Overlay, watermark, intégrations tierces |
| Favicon | `Favicon/logo-favicon_64.svg` | favicon.ico, app icon |

Chaque variante a un sous-dossier `PNG/` et `SVG/` avec exports déclinés en taille (Inkscape, manuels).

À la première impl :
- Copier `Favicon/logo-favicon_64.svg` (et PNG) dans `static/favicon.ico` + `static/assets/icons/`
- Copier les variantes Dark + WhiteMode dans `static/assets/logo/` selon le mode actif
- Pour Android : copier dans `android-app/src/main/res/drawable/` selon convention Android density

## Décisions clés

1. **Light mode obligatoire** — Nightfall v1 (loom) était dark-only. v2 livre **les deux** ; toggle utilisateur (préf `localStorage` `ds-theme: "light"|"dark"|"system"`, default `system` qui suit `prefers-color-scheme`).
2. **Tokens CSS variables** — implémentation web via CSS custom properties (`--ds-*`) sur `:root[data-theme="light"]` / `:root[data-theme="dark"]`. Pas de SASS. Pas de tailwind theme config — les tokens sont la source.
3. **Compose Material 3** côté Android — palette mappée vers `MaterialTheme.colorScheme` (primary=`#0E9EB0`, secondary=`#D37C04`, tertiary=`#07BCD3`). Themes Light + Dark séparés.
4. **Halo plat dark mode** — réservé aux assets vectoriels (logo SVG). Pour les viz dataviz (D3/Canvas), on utilise les tokens flat sans halo (perf rendering).
5. **Pas de neon** — décision validée Vectorizer 2026-04-25 : flat over neon. Aucun glow, aucune ombre, aucun filtre SVG.
6. **Source assets externe** — Vectorizer reste un repo séparé. SamsungHealth bundle copies-as-of-spec. Pas de git submodule (overhead inutile pour assets stables).

## Convention de consommation

Toute spec frontend qui touche un de ces périmètres :
- `static/**` (web Nightfall)
- `android-app/src/main/**` (Android shell + dashboard)
- Tout nouveau composant UI (Compose Canvas, web component)

… **doit** inclure dans son frontmatter :

```yaml
related_specs:
  - 2026-04-26-nightfall-rebrand-data-saillance
```

L'agent `code-cartographer` rendra le lien automatiquement dans la note vault `docs/vault/code/<file>.md`. L'agent `plan-keeper` flaggera comme déviation toute spec frontend qui ne référence pas cette ref.

## Out of scope de cette spec (couvert ailleurs)

- Implémentation du theme switcher (CSS / JS / Compose) — sera la première spec frontend qui consomme cette ref
- Animation transition light↔dark — décision déférée à la première impl
- Charte motion (durations, easings) — pas dans Vectorizer aujourd'hui, à compléter si besoin
- Iconographie (au-delà du favicon) — pas figée, à standardiser quand un besoin émerge

## Suite naturelle

- **Première spec frontend post-V2.3** (probablement V2.3.3 frontend Nightfall login form ou un V3.0 web shell) : crée le toggle theme + intègre les CSS vars + bundle 1 logo + 2 polices. C'est elle qui valide en pratique la portabilité de cette ref.
- **Phase 4 master plan** (Android shell + WebView dashboard) : consomme cette ref via `MaterialTheme` Compose + WebView qui hérite des CSS vars web.
- **Phase 5 master plan** (Compose Canvas natif dashboard) : consomme cette ref via tokens couleur Compose.

## Sync avec Vectorizer

Workflow quand Vectorizer évolue :
1. Tu commits le changement dans `~/MyPersonalProjects/Vectorizer/`
2. Tu m'appelles avec le scope du changement (nouvelle palette, nouveau logo, etc.)
3. Je propose un patch de cette spec (section concernée) + un patch des assets bundlés dans `static/assets/` ou `android-app/.../res/`
4. PR dédiée `chore/sync-data-saillance-vN`

Pas de sync automatique — sync explicite garde la traçabilité de quand/pourquoi le bundle a bougé.

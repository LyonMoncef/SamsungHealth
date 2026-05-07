---
name: spec
description: Génère un squelette de spec dans `docs/vault/specs/<YYYY-MM-DD>-<slug>.md` avec frontmatter pré-rempli (type/status/created/related_plans/implements/tested_by) + body template (Vision/Décisions/Livrables/Tests d'acceptation). Granularité cible : 1 spec ≈ 1 feature ou User Story technique. Une spec par PR.
allowed-tools: ["Bash", "Write", "Read"]
next_default: /tdd
---

Tu créés un squelette de spec. Tu **n'écris pas la spec** — tu poses la structure que l'humain (ou `spec-writer`) va remplir.

## Args supportés

`/spec <slug> [--type spec|us|feature|plan] [--related <plan-slug>]`

- `slug` : kebab-case (ex: `postgres-sleep-table`, `auth-jwt-flow`)
- `--type` : défaut `spec` (autres : `us`, `feature`, `plan`)
- `--related` : slug d'un plan parent (ex: `2026-04-23-plan-v2-refactor-master`)

## Steps

### 1. Valider le slug

```bash
SLUG="<slug>"
echo "$SLUG" | grep -qE '^[a-z0-9][a-z0-9-]{2,40}$' || {
  echo "❌ Slug invalide. Format: kebab-case 3-41 chars."
  exit 1
}
DATE=$(date +%Y-%m-%d)
TYPE="${TYPE:-spec}"
PATH="docs/vault/specs/${DATE}-${SLUG}.md"

if [ -f "$PATH" ]; then
  echo "❌ Spec existe déjà : $PATH"
  exit 1
fi
```

### 2. Écrire le squelette

```markdown
---
type: spec  # ou us | feature | plan
title: "<title>"
slug: <date>-<slug>
status: draft  # → ready → in_progress → delivered
created: <date>
delivered: null
priority: medium  # high | medium | low
related_plans:
  - <related-plan-slug>  # si fourni
implements: []
  # - file: server/x.py
  #   symbols: [foo, bar]
  #   line_range: null
tested_by: []
  # - file: tests/test_x.py
  #   classes: [TestFoo]
  #   methods: [test_y]
tags: [<scope>]
---

# Spec — <title humain>

## Vision

<2-3 phrases : qu'est-ce qu'on veut livrer, pour qui, pourquoi maintenant>

## Décisions techniques

- <Décision 1 + bref rationale>
- <Décision 2>

## Livrables

- [ ] <Livrable 1 — fichier ou capability>
- [ ] <Livrable 2>

## Tests d'acceptation

Décrits comme "given/when/then" pour identifier les classes/méthodes de test à créer côté `tested_by:`.

1. **<Cas 1>** — given <X>, when <Y>, then <Z>
2. **<Cas 2>** — ...

## Suite naturelle

<Phase suivante / spec dépendante>
```

### 3. Confirmer + propose le next

```
✅ Spec créée : docs/vault/specs/<date>-<slug>.md
   Type: <type>, status: draft

📝 Édite la spec maintenant (ouvrir dans Obsidian/VSCode).
👉 Next: /vision docs/vault/specs/<date>-<slug>.md   ← audit alignement VISION.md
   Puis : /tdd docs/vault/specs/<date>-<slug>.md
```

## Règles

- **Ne pas remplir la spec** — squelette seulement. L'humain ou `spec-writer` le fait.
- Refuser si fichier existe (collision = renommer slug).
- Frontmatter `implements:` et `tested_by:` doivent être des **listes** (vides ou peuplées) — pas `null`.
- `related_plans:` pointe vers un plan parent quand applicable (Phase A.8 → `2026-04-23-plan-v2-multi-agents-architecture`).
- **Granularité** : 1 spec ≈ 1 feature/US technique livrable en < 1 semaine. Si > 1 semaine, c'est un `plan` qui se découpe en N specs enfants.
- Un test = N specs validées (rare) ; une spec = N tests (commun) — déclaré dans `tested_by:` côté spec.

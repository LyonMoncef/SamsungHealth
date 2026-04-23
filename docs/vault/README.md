# SamsungHealth — Code-as-vault

Miroir Obsidian de la codebase + annotations ancrées. Voir [plan complet](../../../mnt/c/Users/idsmf/Documents/PKM/vault/02_Projects/SamsungHealth/specs/2026-04-23-plan-code-as-vault.md) (Obsidian) ou `/home/tats/.claude/plans/expressive-napping-blum.md` (Claude Code).

## Comment ouvrir

⚠️ **Repo sous WSL** : Obsidian Windows ne peut pas watch les paths `\\wsl.localhost\...` (chokidar EISDIR). Solution adoptée : **mirror sens unique vers Windows**.

### Setup

```bash
# 1. Définir le path mirror Windows (en WSL)
export CARTOGRAPHER_MIRROR_TO=/mnt/c/Users/idsmf/Documents/PKM/vault/02_Projects/SamsungHealth
# (ajouter dans ~/.zshrc ou ~/.bashrc pour rendre permanent)

# 2. Bootstrap initial
make vault-mirror

# 3. À partir de ce moment, le hook pre-commit re-mirror automatiquement à chaque commit
```

### Côté Obsidian Windows

Dans ton vault PKM principal (`C:\Users\idsmf\Documents\PKM\vault\`), le dossier `02_Projects/SamsungHealth/` **EST** le mirror : même structure que `docs/vault/` du repo (`code/`, `specs/`, `changelog/`, `_index/`, `annotations/`, `codex/`, `_template/`, `assets/`, `MIRROR-README.md`). Pas de second vault à ouvrir, recherche/graphe unifiés avec ton PKM.

**Important** : tout ce qui vit sous `02_Projects/SamsungHealth/` côté PKM est généré depuis le repo. Toute édition manuelle est perdue au prochain sync. Le contenu source-of-truth vit dans `SamsungHealth/docs/vault/` côté repo WSL.

### Règles de l'usage mirror

- **Le mirror est read-only** : toute édition côté Windows est perdue au prochain sync. Voir `MIRROR-README.md` à la racine du mirror.
- **Édition d'annotation** : faire côté repo WSL via `/annotate edit <slug>` (ouvre `docs/vault/annotations/<...>/<slug>.md`) ou éditer directement le fichier en VSCode/nano. L'agent `code-cartographer` se charge habituellement de cette édition.
- **Code source** : édité dans le repo (jamais dans le mirror). La note `code/<...>.md` est régénérée à chaque commit.

## Structure

```
docs/vault/
├── README.md                            ← ce fichier
├── code/                                ← notes miroirs (1 par fichier source) — AUTO-GÉNÉRÉES
│   ├── server/
│   ├── agents/
│   ├── scripts/
│   ├── static/
│   └── android-app/
├── annotations/                         ← contenu source des annotations — humain + agent
│   ├── <package>/<file>/<slug>.md       ← une annotation par slug
│   ├── _cross/                          ← annotations multi-fichiers (même slug dans plusieurs files)
│   └── _orphans/                        ← annotations dont le marqueur a été perdu (à résoudre via /anchor-review)
├── changelog/                           ← 1 note par commit (auto-généré depuis git log)
├── logs/                                ← Phase B+, gitignored
├── assets/                              ← images embarquées dans annotations (PNG, SVG)
└── _index/
    ├── orphans.md                       ← liste des orphans actifs
    ├── coverage.md                      ← fichiers source sans annotation
    └── annotations-by-tag.md            ← reverse index par tags frontmatter
```

## Marqueurs d'annotation dans le code source

Chaque annotation dans `annotations/<...>/<slug>.md` est ancrée à un emplacement précis du code source via un commentaire spécial.

### Syntaxe par langage

| Langage | Single line | Range begin/end |
|---------|-------------|-----------------|
| Python | `# @vault:slug` | `# @vault:slug begin` ... `# @vault:slug end` |
| JavaScript | `// @vault:slug` | `// @vault:slug begin` ... `// @vault:slug end` |
| Kotlin | `// @vault:slug` | `// @vault:slug begin` ... `// @vault:slug end` |
| HTML | `<!-- @vault:slug -->` | `<!-- @vault:slug begin -->` ... `<!-- @vault:slug end -->` |
| CSS | `/* @vault:slug */` | `/* @vault:slug begin */` ... `/* @vault:slug end */` |

### Slug syntax

`[a-z0-9][a-z0-9-]{2,40}` — kebab-case, 3-41 caractères.

Bon : `sleep-perf-cap`, `n1-query-risk`, `auth-jwt-flow`
Mauvais : `Sleep_Perf` (capitales/underscores), `x` (trop court), `_temp` (réservé aux orphans)

### 4 scopes supportés

**A — 1 ligne (commentaire de fin de ligne)**
```python
limit: int = Query(30, gt=0, le=1000),  # @vault:sleep-perf-cap
```

**B — Range begin/end**
```python
# @vault:sleep-pipeline begin
async def get_sessions(...):
    ...
# @vault:sleep-pipeline end
```

**C — Non-contigu (même slug, plusieurs paires begin/end dans le même fichier)**
```python
# @vault:n1-query-risk begin
class SleepCalculator: ...
# @vault:n1-query-risk end

# ... 100 lignes ...

# @vault:n1-query-risk begin
def helper_calc(): ...
# @vault:n1-query-risk end
```

**D — Cross-file (même slug dans plusieurs fichiers)**
```python
# server/routers/sleep.py
def get_sessions(...):  # @vault:sleep-stages-pattern
```
```python
# server/database.py
def get_sleep_stages(...):  # @vault:sleep-stages-pattern
```
→ annotation file dans `annotations/_cross/sleep-stages-pattern.md`

## Cycle de vie

1. **Création** — `/annotate <slug> --at <file:line>` ou `--range <file:start-end>`
2. **Édition** — éditer le fichier `annotations/<...>/<slug>.md` directement dans Obsidian (markdown libre : texte, images, wikilinks, callouts, tableaux)
3. **Resync au commit** — pre-commit hook regénère les notes `code/` depuis (code source + annotations)
4. **Détection orphan** — si marqueur supprimé du code → `status: orphan`, déplacé dans `annotations/_orphans/`, callout warning visible en haut de la note vault
5. **Résolution** — `/anchor-review <slug>` propose nouvelle position via fuzzy match
6. **Suppression** — `/annotate delete <slug>` retire annotation + marqueurs

## Skills disponibles

| Skill | Usage |
|-------|-------|
| `/sync-vault` | Régénère toutes les notes vault depuis code+annotations (mode `--full`, `--diff`, `--check`) |
| `/annotate <slug> --at <file:line>` | Crée une annotation single-line |
| `/annotate <slug> --range <file:start-end>` | Crée une annotation sur un range |
| `/annotate edit <slug>` | Ouvre le fichier annotation pour édition |
| `/annotate delete <slug>` | Supprime annotation + marqueurs |
| `/anchor-review [<slug>]` | Résout les annotations orphelines |

## Conventions

- **Code = source de vérité** : la note vault est régénérée, ne JAMAIS l'éditer directement
- **Annotations = source de vérité humaine** : édite les fichiers `annotations/<slug>.md` librement, ils survivent aux regen
- **Sync sens unique** : code+annotations → note vault rendue (jamais l'inverse)
- **Frontmatter standard** :
  - notes `code/` : `type: code-source`, `language`, `file_path`, `git_blob`, `last_synced`, `annotations[]`
  - notes `annotations/` : `slug`, `type: annotation`, `created`, `created_by`, `last_verified`, `status`, `anchors[]`, `references{}`, `tags[]`

## Réf agent

Maintenu par `code-cartographer` (`.claude/agents/code-cartographer.md`). Voir le contrat Pydantic `agents/contracts/cartographer.py`.

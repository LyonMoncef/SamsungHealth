---
name: annotate
description: CRUD sur les annotations vault. `/annotate <slug> --at <file:line>` (single), `--range <file:start-end>` (range), `edit <slug>` (ouvre pour édition), `delete <slug>` (supprime annotation + marqueurs). Slug doit matcher `^[a-z0-9][a-z0-9-]{2,40}$`. Invoque code-cartographer pour le marker injection + re-render.
allowed-tools: ["Bash", "Read", "Write", "Edit"]
next_default: /sync-vault --diff
---

Tu gères le cycle de vie des annotations : création, édition, suppression. La position des marqueurs dans le code est gérée par `marker_injector` ; le contenu de l'annotation par `annotation_io`. Après chaque op tu déclenches une re-render.

## Args supportés

| Forme | Action |
|-------|--------|
| `<slug> --at <file>:<line>` | Crée annotation single + marker EOL à la ligne |
| `<slug> --range <file>:<start>-<end>` | Crée annotation range + marqueurs begin/end |
| `edit <slug>` | Affiche le path du fichier annotation pour édition manuelle dans Obsidian |
| `delete <slug>` | Supprime annotation file + tous les marqueurs du code |

## Steps — création (`--at` ou `--range`)

### 1. Valider le slug

```bash
SLUG="<slug>"
echo "$SLUG" | grep -qE '^[a-z0-9][a-z0-9-]{2,40}$' || {
  echo "❌ Slug invalide. Format: kebab-case 3-41 chars (ex: sleep-perf-cap)"
  exit 1
}
```

### 2. Détecter le langage du fichier cible

```bash
FILE="<file>"
case "$FILE" in
  *.py) LANG="python" ;;
  *.js|*.mjs|*.cjs) LANG="javascript" ;;
  *.kt|*.kts) LANG="kotlin" ;;
  *.html|*.htm) LANG="html" ;;
  *.css) LANG="css" ;;
  *) echo "❌ Extension non supportée"; exit 1 ;;
esac
```

### 3. Vérifier qu'aucune annotation `<slug>` n'existe déjà

```bash
EXISTING=$(grep -rl "^slug: $SLUG\$" docs/vault/annotations/ 2>/dev/null)
if [ -n "$EXISTING" ]; then
  echo "⚠️  Slug déjà utilisé : $EXISTING"
  echo "Si même contexte → ajouter une 2e ancre (cross-file). Sinon choisir un autre slug."
  exit 1
fi
```

### 4. Inject marker + écrire annotation file

Utilise un script Python inline qui appelle directement les modules :

```bash
python3 - <<PYEOF
from agents.cartographer.marker_injector import inject_single, inject_range
from agents.cartographer.annotation_io import resolve_annotation_path, write_annotation
from agents.contracts.cartographer import AnchorLocation

slug = "$SLUG"
file = "$FILE"
lang = "$LANG"

# Single OR range — selon les flags passés au skill
if "$AT_LINE":
    line = int("$AT_LINE")
    inject_single(file, line, slug, lang)
    anchors = [AnchorLocation(file=file, kind="single", line=line)]
else:
    begin, end = int("$RANGE_BEGIN"), int("$RANGE_END")
    inject_range(file, begin, end, slug, lang)
    anchors = [AnchorLocation(file=file, kind="range", begin_line=begin, end_line=end)]

path = resolve_annotation_path(slug, anchors, "docs/vault")
write_annotation(
    path=path, slug=slug, anchors=anchors,
    scope="single-file", created_by="human",
    body=f"# {slug}\n\nDécrire ici le pourquoi.\n",
)
print(f"✓ Annotation créée : {path}")
print(f"✓ Marker injecté dans : {file}")
PYEOF
```

### 5. Re-render note vault

```bash
python3 -m agents.cartographer.cli --diff "$FILE"
```

### 6. Affiche le path pour édition

```
✅ Annotation '<slug>' créée.
📝 Édite le contenu : <path>
👉 Next: /sync-vault --diff (après édition) ou /commit
```

## Steps — édition (`edit <slug>`)

```bash
PATH=$(grep -rl "^slug: $SLUG\$" docs/vault/annotations/ | head -1)
[ -z "$PATH" ] && echo "❌ Annotation introuvable" && exit 1
echo "📝 Édite : $PATH"
echo "👉 Après édition : /sync-vault --diff"
```

## Steps — suppression (`delete <slug>`)

```bash
PATH=$(grep -rl "^slug: $SLUG\$" docs/vault/annotations/ | head -1)
[ -z "$PATH" ] && echo "❌ Annotation introuvable" && exit 1

# Lire les ancres pour savoir quels fichiers source toucher
FILES=$(python3 -c "
from agents.cartographer.annotation_io import read_annotation
meta, _ = read_annotation('$PATH')
for a in meta.get('anchors', []):
    print(a['file'])
")

# Remove markers
python3 - <<PYEOF
from agents.cartographer.marker_injector import remove_marker
from agents.cartographer.markers import infer_language
for f in """$FILES""".strip().split("\n"):
    lang = infer_language(f)
    if lang: remove_marker(f, "$SLUG", lang)
PYEOF

rm "$PATH"
python3 -m agents.cartographer.cli --diff $FILES
echo "✅ Annotation '$SLUG' supprimée + marqueurs retirés."
```

## Délivrable

```
✅ annotate <op> '<slug>' — <N> fichiers modifiés
👉 Next: /sync-vault --diff (édition manuelle) ou /commit
```

## Règles strictes

- Slug regex stricte : `^[a-z0-9][a-z0-9-]{2,40}$`. Refuser sinon.
- Pas de création silencieuse d'un slug déjà utilisé (la collision force `_cross/` ou rename).
- `delete` est destructif : confirmer avant si `--no-confirm` n'est pas explicite.

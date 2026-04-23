---
name: anchor-review
description: Résout les annotations orphelines. Sans arg → liste les orphans. Avec `<slug>` → tente fuzzy-match sur le code, propose 1-3 emplacements candidats, attend validation humaine pour ré-injecter le marker.
allowed-tools: ["Bash", "Read", "Grep"]
next_default: /commit
---

Tu redonnes une ancre aux annotations orphelines. Tu ne ré-injectes JAMAIS un marker sans confirmation explicite — la mauvaise position peut polluer le code.

## Args supportés

- (aucun) → liste les orphans actifs depuis `docs/vault/annotations/_orphans/`
- `<slug>` → résout cet orphan spécifique

## Steps — sans arg : list orphans

```bash
ORPHAN_DIR="docs/vault/annotations/_orphans"
if [ ! -d "$ORPHAN_DIR" ] || [ -z "$(ls -A $ORPHAN_DIR 2>/dev/null)" ]; then
  echo "✅ Aucun orphan."
  exit 0
fi

echo "📋 Orphans actifs :"
for f in "$ORPHAN_DIR"/*.md; do
  SLUG=$(grep '^slug:' "$f" | head -1 | awk '{print $2}')
  ANCHOR_FILE=$(grep -A2 'anchors:' "$f" | grep 'file:' | head -1 | awk '{print $2}')
  echo "  - $SLUG (ancien fichier : $ANCHOR_FILE) → /anchor-review $SLUG"
done
```

## Steps — avec `<slug>` : résolution

### 1. Charger l'annotation orpheline

```bash
SLUG="<slug>"
ORPHAN="docs/vault/annotations/_orphans/$SLUG.md"
[ ! -f "$ORPHAN" ] && echo "❌ Orphan '$SLUG' introuvable" && exit 1

# Extraire l'ancien fichier + body pour fuzzy match
OLD_FILE=$(python3 -c "
from agents.cartographer.annotation_io import read_annotation
meta, _ = read_annotation('$ORPHAN')
for a in meta.get('anchors', []):
    print(a['file']); break
")
BODY=$(python3 -c "
from agents.cartographer.annotation_io import read_annotation
_, body = read_annotation('$ORPHAN')
print(body[:300])
")

echo "🔍 Orphan : $SLUG"
echo "   Ancien fichier : $OLD_FILE"
echo "   Body extract :"
echo "$BODY" | sed 's/^/     /'
```

### 2. Proposer des emplacements candidats

Utilise Grep + Read pour identifier 1-3 emplacements pertinents :

- Si `OLD_FILE` existe encore → propose les symboles dont le nom apparait dans le body de l'annotation
- Si `OLD_FILE` n'existe plus → grep le repo pour les keywords du body (function names mentionnés)
- Toujours montrer 5-10 lignes de contexte autour de chaque candidat

Format :

```
Candidat 1 : server/routers/sleep.py:42 (fonction get_sessions)
  ```python
  @router.get("/api/sleep")
  async def get_sessions(limit, ...):
  ```

Candidat 2 : server/database.py:120
  ...
```

### 3. Demander la validation humaine

```
Choix :
  [1-3] : ré-injecte le marker à ce candidat (single line par défaut)
  range <file>:<start>-<end> : précise une range manuelle
  skip : laisse en orphan
```

**N'exécute aucune injection avant la réponse humaine.**

### 4. Une fois validé

```bash
python3 - <<PYEOF
from agents.cartographer.marker_injector import inject_single, inject_range
from agents.cartographer.annotation_io import (
    read_annotation, resolve_annotation_path, write_annotation, update_status,
)
from agents.cartographer.markers import infer_language
from agents.contracts.cartographer import AnchorLocation
import os

slug = "$SLUG"
file = "$CHOSEN_FILE"
lang = infer_language(file)

# Injecter selon le choix
if "$CHOSEN_KIND" == "single":
    inject_single(file, int("$CHOSEN_LINE"), slug, lang)
    anchors = [AnchorLocation(file=file, kind="single", line=int("$CHOSEN_LINE"))]
else:
    b, e = int("$CHOSEN_BEGIN"), int("$CHOSEN_END")
    inject_range(file, b, e, slug, lang)
    anchors = [AnchorLocation(file=file, kind="range", begin_line=b, end_line=e)]

# Déplacer le fichier annotation hors de _orphans/ + update status
old_path = "$ORPHAN"
meta, body = read_annotation(old_path)
new_path = resolve_annotation_path(slug, anchors, "docs/vault")
write_annotation(
    path=new_path, slug=slug, anchors=anchors,
    scope=meta.get("scope", "single-file"),
    created_by=meta.get("created_by", "human"),
    body=body,
    references=meta.get("references", {}),
    tags=meta.get("tags", []),
    status="active",
)
os.remove(old_path)
print(f"✓ Annotation déplacée : {old_path} → {new_path}")
print(f"✓ Marker ré-injecté dans : {file}")
PYEOF

# Re-render
python3 -m agents.cartographer.cli --diff "$CHOSEN_FILE"
```

## Délivrable

```
✅ anchor-review '<slug>' → résolu (<file>:<loc>)
👉 Next: /commit
```

Si skip :
```
⏭ '<slug>' laissé en orphan. Re-tenter plus tard via /anchor-review <slug>.
```

## Règles strictes

- **Ne jamais ré-injecter sans confirmation humaine** — proposer ≠ exécuter.
- Si plusieurs orphans → traiter un par un (interactif), pas en batch.
- Si le candidat choisi a déjà un autre slug à la même ligne → warner et proposer une autre ligne (collision EOL).
- L'annotation file conserve son contenu (frontmatter `created`, body, tags) — seul `status: active` + nouvelles `anchors:` sont mis à jour.

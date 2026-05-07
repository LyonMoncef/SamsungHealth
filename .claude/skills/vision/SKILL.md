---
name: vision
description: Invoque vision-keeper pour auditer l'alignement d'une spec ou d'un plan avec VISION.md. Produit un verdict aligned/drift_alert/vision_update_needed et un score 0-100. Bloquant si finding severity=block.
allowed-tools: ["Bash", "Read", "Write", "Agent"]
next_default: /tdd
---

Tu orchestres l'audit de vision. Tu ne lis pas la spec toi-même — tu délègues à `vision-keeper`.

## Args

`/vision <artifact-path> [--slug <slug>]`

- `artifact-path` : chemin vers la spec, plan ou diff à auditer
- `--slug` : identifiant court (défaut : basename sans extension)

## Steps

### 1. Préparer le brief

```bash
ARTIFACT_PATH="<artifact-path>"
SLUG="${SLUG:-$(basename $ARTIFACT_PATH .md)}"
WORK_DIR="${CLAUDE_PROJECT_DIR:-.}/.claude/work/vision-$SLUG"
mkdir -p "$WORK_DIR"
```

### 2. Déléguer à vision-keeper

Invoque le subagent avec :
- `artifact_path` = chemin absolu vers la spec
- `slug` = slug court
- `vision_path` = `VISION.md` (racine projet)
- `work_dir` = `$WORK_DIR`

### 3. Lire le résultat et afficher

Lis `$WORK_DIR/vision-audit.md` (rapport humain).

Affiche :

```
Vision: <verdict> (score <N>/100) — <slug>
<Si drift_alert>
⚠️  Dérives : <liste des principles violés>
Rapport : .claude/work/vision-<slug>/vision-audit.md
👉 Next: corriger la spec puis re-run /vision, ou /tdd si aligned.
<Si aligned>
✅ Aligned — pas de dérive détectée.
👉 Next: /tdd <artifact-path>
<Si block>
❌ BLOCK — violation hard (C1 ou LLM). Corriger avant /tdd.
```

## Règles

- Si verdict = `block` → STOP. Ne pas passer à `/tdd`.
- Si verdict = `drift_alert` → afficher le patch suggéré, laisser l'humain décider.
- Si VISION.md absent → verdict `aligned` par défaut, continuer.
- Read-only — ce skill ne modifie jamais la spec.

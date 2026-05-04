---
name: tdd
description: Invoque `test-writer` pour générer les tests RED depuis une spec validée. Vérifie qu'ils sont bien rouges (collection OK + échecs attendus) avant de rendre la main. Respecte le contrat `TestBrief` → `TestArtifact`. Prérequis — spec status ≥ ready, frontmatter `tested_by:` rempli.
allowed-tools: ["Bash", "Read", "Write"]
next_default: /impl
---

Tu es le skill qui lance l'étape **red** du cycle TDD. Tu ne rédiges pas les tests toi-même — tu prépares le brief + délègues au subagent `test-writer`.

## Args supportés

`/tdd <spec-path> [--target-dir <path>]`

- `spec-path` — chemin vers la spec (ex: `docs/vault/specs/2026-04-24-v2-postgres-migration.md`)
- `--target-dir` : défaut auto-détecté depuis frontmatter `tested_by:` (premier `file:` → on extrait son dir parent). Override si la spec couvre plusieurs dirs et que tu veux scoper.

## Steps

### 1. Valider les prérequis

```bash
SPEC="$1"
[ -f "$SPEC" ] || { echo "❌ Spec introuvable : $SPEC"; exit 1; }

# Spec status doit être ≥ ready
META=$(python3 -c "
import yaml
with open('$SPEC') as f:
    fm = f.read().split('---', 2)[1]
print(yaml.safe_dump(yaml.safe_load(fm) or {}))
")
STATUS=$(echo "$META" | python3 -c "import sys,yaml; print((yaml.safe_load(sys.stdin) or {}).get('status', 'draft'))")
case "$STATUS" in
  ready|in_progress|approved) ;;
  *) echo "❌ Spec status=$STATUS — passe à 'ready' avant /tdd"; exit 1 ;;
esac

# tested_by[] doit être non-vide
TESTED_BY=$(echo "$META" | python3 -c "
import sys, yaml, json
m = yaml.safe_load(sys.stdin) or {}
tb = m.get('tested_by') or []
print(json.dumps(tb))
")
[ "$TESTED_BY" = "[]" ] && {
  echo "❌ Frontmatter tested_by[] vide — déclare au moins un fichier de test cible avant /tdd"
  exit 1
}
```

### 2. Résoudre `target_test_dir`

Si `--target-dir` fourni → utiliser tel quel. Sinon, prendre le `dirname` du premier `tested_by[].file`.

```bash
if [ -z "$TARGET_DIR" ]; then
  TARGET_DIR=$(echo "$TESTED_BY" | python3 -c "
import sys, json, os
tb = json.load(sys.stdin)
first = tb[0].get('file', '') if isinstance(tb[0], dict) else tb[0]
print(os.path.dirname(first))
")
fi
[ -n "$TARGET_DIR" ] || { echo "❌ Impossible de résoudre target_test_dir"; exit 1; }
mkdir -p "$TARGET_DIR"
```

### 3. Préparer le `brief.json` (contrat `TestBrief`)

```bash
WORK_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}/.claude/work/tdd-$(date +%s)"
mkdir -p "$WORK_DIR"

python3 -c "
import json
brief = {
    'spec_path': '$SPEC',
    'target_test_dir': '$TARGET_DIR',
}
with open('$WORK_DIR/brief.json', 'w') as f:
    json.dump(brief, f, indent=2)
"
```

### 4. Déléguer au subagent `test-writer`

Invoque l'agent via le mécanisme de délégation (Agent tool côté main session). Le subagent :
- Lit `${WORK_DIR}/brief.json`
- Lit la spec, extrait les cas d'acceptation
- Écrit les tests dans `target_test_dir`
- Lance `pytest <target_test_dir> -v` pour valider qu'ils sont **RED**
- Produit `${WORK_DIR}/result.json` (contrat `TestArtifact`)

### 5. Vérifier RED + présenter le résultat

Lis `${WORK_DIR}/result.json`. Affiche :

```
✅ TDD delivery — <N> tests RED écrits dans <target_test_dir>
   Fichiers : <test_files>
   RED : <tests_red_count>, GREEN : <tests_green_count>
👉 Next: /impl <spec-path>
```

**Garde-fou** : si `tests_green_count > 0` avant impl, c'est un bug du subagent (test mal écrit qui passe sans implémentation). Affiche un warning explicite et propose de retravailler les tests concernés.

Si `status: needs_clarification` ou `failed` → affiche les `blockers` et STOP.

## Règles

- **Ne rédige pas les tests toi-même** — délégation stricte au subagent
- **Ne touche pas `server/` ni `static/`** — c'est le rôle de `coder-*` via `/impl`
- Si un test passe GREEN avant impl → c'est un faux test, le subagent doit le réécrire
- Laisse `$WORK_DIR` en place — `/impl` consommera implicitement les `tests_red_path` via la spec, pas via le `result.json`
- Si la spec déclare plusieurs `tested_by[]` dans des dirs différents : un seul `target_test_dir` = celui du premier ; les autres se feront en sessions `/tdd` séparées (ou via override `--target-dir`)

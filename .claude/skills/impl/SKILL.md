---
name: impl
description: Invoque `coder-backend` (ou `coder-android`, `coder-frontend`) pour implémenter le minimum de code faisant passer des tests RED. Respecte le contrat `CodeBrief` → `CodeArtifact`. Prérequis — spec existe + tests RED présents. Ne commit pas (c'est git-steward).
allowed-tools: ["Bash", "Read", "Write"]
next_default: /review
---

Tu es le skill qui lance l'étape **green** du cycle TDD. Tu ne codes pas toi-même — tu prépares le brief + délègues au subagent `coder-<target>`.

## Args supportés

`/impl <spec-path> [--target backend|android|frontend] [--tests <tests-red-path>]`

- `spec-path` — chemin vers la spec (ex: `docs/vault/specs/2026-04-24-v2-postgres-migration.md`)
- `--target` : défaut `backend` ; `android` ou `frontend` si la spec cible ces scopes
- `--tests` : chemin explicite vers les tests RED ; sinon auto-détection via frontmatter `tested_by:` de la spec

## Steps

### 1. Valider les prérequis

```bash
SPEC="$1"
TARGET="${TARGET:-backend}"

[ -f "$SPEC" ] || { echo "❌ Spec introuvable : $SPEC"; exit 1; }
[ "$TARGET" = "backend" ] || [ "$TARGET" = "android" ] || [ "$TARGET" = "frontend" ] || {
  echo "❌ Target invalide (backend|android|frontend) : $TARGET"
  exit 1
}

# Spec status doit être ≥ ready
STATUS=$(python3 -c "
import yaml, sys
with open('$SPEC') as f:
    txt = f.read()
if not txt.startswith('---'):
    print('no-frontmatter'); sys.exit()
fm = txt.split('---', 2)[1]
meta = yaml.safe_load(fm) or {}
print(meta.get('status', 'draft'))
")
case "$STATUS" in
  ready|in_progress|approved) ;;
  *) echo "❌ Spec status=$STATUS — passe à 'ready' avant /impl"; exit 1 ;;
esac
```

### 2. Résoudre les tests RED

Si `--tests` fourni → utiliser tel quel. Sinon, extraire `tested_by[]` du frontmatter spec → prendre le premier fichier `file:` comme `tests_red_path`.

```bash
if [ -z "$TESTS_RED" ]; then
  TESTS_RED=$(python3 -c "
import yaml
with open('$SPEC') as f:
    fm = f.read().split('---', 2)[1]
meta = yaml.safe_load(fm) or {}
tb = meta.get('tested_by') or []
if tb and isinstance(tb, list):
    print(tb[0].get('file', '') if isinstance(tb[0], dict) else tb[0])
")
fi

[ -f "$TESTS_RED" ] || {
  echo "❌ tests RED introuvables : $TESTS_RED"
  echo "   → lance /tdd $SPEC pour les générer"
  exit 1
}
```

### 3. Vérifier que les tests sont bien RED

```bash
python3 -m pytest "$TESTS_RED" -x --tb=no -q
EXIT=$?
if [ $EXIT -eq 0 ]; then
  echo "⚠️  Les tests passent déjà — rien à implémenter, ou tests vides. Revoir $TESTS_RED."
  exit 1
fi
```

### 4. Préparer le `brief.json` (contrat `CodeBrief`)

```bash
WORK_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}/.claude/work/impl-$(date +%s)"
mkdir -p "$WORK_DIR"

# Extraction implements[] pour target_files
TARGET_FILES=$(python3 -c "
import yaml, json
with open('$SPEC') as f:
    fm = f.read().split('---', 2)[1]
meta = yaml.safe_load(fm) or {}
impl = meta.get('implements') or []
files = [x.get('file') for x in impl if isinstance(x, dict) and x.get('file')]
print(json.dumps(files))
")

# Constraints = body 'Décisions techniques' simplifié (l'agent relit la spec)
python3 -c "
import json
brief = {
    'spec_path': '$SPEC',
    'target': '$TARGET',
    'target_files': $TARGET_FILES,
    'constraints': [],
    'tests_red_path': '$TESTS_RED',
}
with open('$WORK_DIR/brief.json', 'w') as f:
    json.dump(brief, f, indent=2)
"
```

### 5. Déléguer au subagent `coder-<target>`

Invoque l'agent via le mécanisme de délégation (Agent tool côté main session). Le subagent :
- Lit `${WORK_DIR}/brief.json`
- Implémente jusqu'à GREEN sur `tests_red_path`
- Vérifie qu'aucun autre test n'est cassé
- Produit `${WORK_DIR}/diff.patch`, `${WORK_DIR}/test-output.txt`, `${WORK_DIR}/result.json` (contrat `CodeArtifact`)

### 6. Présenter le résultat

Lis `${WORK_DIR}/result.json`. Affiche :

```
✅ Impl delivery — <N> fichiers modifiés, tests GREEN (<count>), lint <clean|warn>
   Diff : <WORK_DIR>/diff.patch
👉 Next: /review
```

Si `status: needs_clarification` ou `failed` → affiche les `blockers` et STOP (pas de next_recommended automatique).

## Règles

- **Ne modifie pas le code toi-même** — délégation stricte au subagent
- **Ne commit pas** — c'est le rôle de `git-steward` via `/commit`
- Si target=android ou frontend et que le subagent n'existe pas encore (`.claude/agents/coder-android.md` absent), échoue explicitement avec "subagent coder-<target> non disponible, Phase X requise"
- Si tests RED déjà GREEN avant implémentation → abort (signal d'un faux RED)
- Laisse `$WORK_DIR` en place après exécution — `/review` le lit

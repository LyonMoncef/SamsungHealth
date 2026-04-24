---
name: align
description: Invoque `plan-keeper` pour détecter les déviations entre les plans (master + multi-agents + specs Obsidian) et la réalité actuelle du repo sur la branche courante. Read-only — propose des patches plans, ne modifie rien. À lancer avant /commit pour éviter l'accumulation de dette narrative.
allowed-tools: ["Bash", "Read", "Write"]
next_default: /commit
---

Tu es le skill qui déclenche l'audit `plan ↔ livraison`. Tu ne lis pas les plans toi-même — tu délègues à `plan-keeper`, tu lis son rapport, tu le présentes.

**Lecture de la comparaison** : plan-keeper compare les plans **de la branche courante** à l'**état actuel** des fichiers sur la branche courante. Pas de diff vs `main`, pas de replay historique — uniquement un snapshot de la cohérence *ici et maintenant*.

## Args supportés

`/align [--triggered-by manual|post_delivery|commit] [--scope recent|full] [--threshold info|low|medium|high|critical]`

- `--triggered-by` : défaut `manual`
- `--scope` : défaut `recent` (fichiers modifiés depuis dernier commit) ; `full` = audit complet
- `--threshold` : severity minimale pour passer `overall: block`. Défaut `medium`

## Steps

### 1. Construire `recent_changes` si `--scope recent`

```bash
SCOPE="${SCOPE:-recent}"
TRIGGERED="${TRIGGERED_BY:-manual}"
THRESHOLD="${THRESHOLD:-medium}"

if [ "$SCOPE" = "recent" ]; then
  RECENT=$(git diff --name-only HEAD~1..HEAD 2>/dev/null ; git diff --cached --name-only ; git diff --name-only)
  RECENT=$(echo "$RECENT" | sort -u | grep -v '^$' || true)
else
  RECENT=""
fi
```

### 2. Préparer le `brief.json` (contrat `PlanAuditBrief`)

```bash
WORK_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}/.claude/work/align-$(date +%s)"
mkdir -p "$WORK_DIR"

python3 -c "
import json
recent = '''$RECENT'''.strip().splitlines() if '''$RECENT'''.strip() else []
brief = {
    'triggered_by': '$TRIGGERED',
    'recent_changes': recent,
    'plan_paths': [],
    'severity_threshold': '$THRESHOLD',
}
with open('$WORK_DIR/brief.json', 'w') as f:
    json.dump(brief, f, indent=2)
"
```

### 3. Déléguer à `plan-keeper`

Invoque le subagent via le mécanisme de délégation. Il produira :
- `${WORK_DIR}/plan-audit.md` — rapport humain lisible
- `${WORK_DIR}/result.json` — contrat `PlanAuditReport`

### 4. Présenter le résultat

Lis `result.json`. Affiche un résumé :

```
✅ Plan audit <aligned|drift_detected|block> — <N> déviations (<C>C/<H>H/<M>M/<L>L/<I>I)
   Plans concernés : <plans_needing_update>
   Rapport complet : <WORK_DIR>/plan-audit.md
```

Si `overall: block` → affiche les déviations `severity ≥ threshold` avec leur `proposed_patch` en clair et STOP (pas de `/commit` automatique).

Si `overall: drift_detected` → liste les déviations (severity < threshold) + propose :
- `/commit` pour committer malgré tout (les patches plans restent à appliquer manuellement via `documenter`)
- Ou appliquer les patches d'abord (éditer les plans à la main selon `proposed_patch`)

Si `overall: aligned` → simplement :

```
✅ aligned — aucune déviation détectée
👉 Next: /commit
```

## Règles

- **Read-only strict** — ce skill ne modifie jamais de plan ni de code. plan-keeper propose, l'humain décide.
- Ne pas transformer automatiquement les `proposed_patch` en éditions — c'est au `documenter` de le faire dans un commit séparé, sur demande explicite.
- Si `plan-keeper` retourne `status: failed` (vault vide, misconfig) → abort avec message pédagogique "lance `/sync-vault --full` pour bootstrap le vault, ou vérifie `docs/vault/specs/`"
- Ne pas court-circuiter le `threshold` — si l'utilisateur veut committer malgré un `block`, il doit `--threshold high` (remonter la barre) et re-run, pas forcer à la main.

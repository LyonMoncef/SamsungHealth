---
name: sync-vault
description: Régénère les notes vault docs/vault/code/ depuis (code source + annotations). Modes — `--full` (bootstrap ou recovery, scan tout le repo), `--diff <files>` (incrémental, par défaut staged + modified), `--check` (dry-run, exit 1 si orphans).
allowed-tools: ["Bash", "Read"]
next_default: /commit
---

Tu invoques `code-cartographer` pour resynchroniser le vault. Tu n'écris pas toi-même les notes vault — tu lis le rapport et le présentes.

## Args supportés

- (aucun) → mode `diff` sur les fichiers staged + modified
- `--full` → mode `full` (bootstrap, ~5s sur ce repo)
- `--diff <file...>` → mode `diff` sur la liste explicite
- `--check` → mode `check` (dry-run, exit non-zero si problème)

## Steps

### 1. Détecte les fichiers candidats (mode diff par défaut)

```bash
if [ -z "$ARGS" ] || [ "$ARGS" = "--diff" ]; then
  # Diff par défaut = staged + modified
  FILES=$(git diff --cached --name-only --diff-filter=ACM ; git diff --name-only --diff-filter=ACM)
  FILES=$(echo "$FILES" | grep -E '\.(py|js|kt|html|css)$|docs/vault/annotations/' | sort -u)
  if [ -z "$FILES" ]; then
    echo "Rien à synchroniser (aucun fichier source modifié)."
    exit 0
  fi
  MODE_FLAG="--diff $FILES"
elif [ "$ARGS" = "--full" ]; then
  MODE_FLAG="--full"
elif [ "$ARGS" = "--check" ]; then
  MODE_FLAG="--check"
else
  MODE_FLAG="$ARGS"
fi
```

### 2. Lance le CLI cartographer

```bash
python3 -m agents.cartographer.cli $MODE_FLAG
EXIT=$?
```

### 3. Présente le résultat

Lis la sortie stdout (format `[code-cartographer] mode=<x> overall=<y> notes=<n>/<m> new_orphans=<o> resolved=<r>`).

Si `new_orphans > 0`, propose `/anchor-review` en next_recommended.
Si `parse_errors`, liste les fichiers concernés.
Si `overall=complete` ET `mode=diff` ET `git diff --cached docs/vault/` non-vide → propose `/commit`.

## Délivrable

```
✅ sync-vault <mode> — <N> notes générées, <O> orphans (<R> résolus)
👉 Next: /<commit|anchor-review|fix>
```

Si bloqué (parse error, vault corrompu, tree-sitter absent) : explique en français le problème + commande de recovery.

---
name: sync-coverage
description: Régénère docs/vault/_index/coverage-map.json (gitignored) en exécutant pytest --cov + dynamic context. Sert de source au note_renderer pour afficher les liens "Tested by:" symbol-level et range-level dans les notes vault. ~10s sur ce repo. Suivi recommandé par /sync-vault --full pour propager les liens dans les notes.
allowed-tools: ["Bash"]
next_default: /sync-vault --full
---

Tu lances le pipeline coverage → manifest. Le résultat est consommé par `code-cartographer` au prochain re-render des notes vault.

## Steps

### 1. Vérifier les outils

```bash
python3 -c "import pytest_cov, coverage" || {
  echo "❌ pytest-cov manquant. Installer avec : make install"
  exit 1
}
```

### 2. Lancer le générateur

```bash
python3 -m agents.cartographer.coverage_map
EXIT=$?
```

### 3. Présenter le résultat

Lis la sortie stdout (`[coverage-map] N symbols / M tests / F files → docs/vault/_index/coverage-map.json`).

Si `EXIT != 0` mais que le manifest existe : tests sont en échec (red), coverage partielle est OK et utilisable. Continue.

Si `EXIT != 0` ET pas de manifest : pipeline cassé, log + exit.

### 4. Proposer la suite

Si OK :
```
✅ sync-coverage — N symbols mappés, M tests indexés, F fichiers couverts
👉 Next: /sync-vault --full (pour propager les liens "Tested by:" dans les notes)
```

Si tests rouges :
```
⚠️ sync-coverage partiel — coverage produite malgré tests en échec (à fixer)
👉 Next: /sync-vault --full (les liens existants restent valides) puis /fix
```

## Règles

- Ne commit PAS le manifest (gitignored). Si tu vois `docs/vault/_index/coverage-map.json` dans `git status`, c'est une régression du `.gitignore` — flag-la.
- N'invoque pas `pytest` directement : passe par `agents.cartographer.coverage_map` qui gère le `.coveragerc-cartographer` + le format JSON contexts.
- Pour scope custom : `python3 -m agents.cartographer.coverage_map --cov-target server --test-path tests/test_sleep.py` (one-shot, ne change pas les défauts).

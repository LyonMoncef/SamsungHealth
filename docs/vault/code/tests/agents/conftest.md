---
type: code-source
language: python
file_path: tests/agents/conftest.py
git_blob: c6f240a944a41f22e48357b3ca0b5874fce8d9ec
last_synced: '2026-04-23T10:40:53Z'
loc: 6
annotations: []
imports:
- pytest
exports: []
tags:
- code
- python
---

# tests/agents/conftest.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/agents/conftest.py`](../../../tests/agents/conftest.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
import pytest


@pytest.fixture(autouse=True)
def clean_db():
    yield
```

---

## Appendix — symbols & navigation *(auto)*

### Imports
- `pytest`

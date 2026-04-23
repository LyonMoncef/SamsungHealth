---
type: code-source
language: python
file_path: agents/contracts/test_writer.py
git_blob: b2c03eb4cc885e2a8b74e9172bf0e9496e39d5a9
last_synced: '2026-04-23T08:13:16Z'
loc: 12
annotations: []
imports:
- .base
exports:
- TestBrief
- TestArtifact
tags:
- code
- python
---

# agents/contracts/test_writer.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/test_writer.py`](../../../agents/contracts/test_writer.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from .base import AgentInputBase, AgentOutputBase


class TestBrief(AgentInputBase):
    spec_path: str
    target_test_dir: str


class TestArtifact(AgentOutputBase):
    test_files: list[str]
    tests_red_count: int
    tests_green_count: int
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestBrief` (class) — lines 4-6
- `TestArtifact` (class) — lines 9-12

### Imports
- `.base`

### Exports
- `TestBrief`
- `TestArtifact`

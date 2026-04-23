---
type: code-source
language: python
file_path: agents/contracts/documenter.py
git_blob: a6e5bc238d1b80e6c36b7b2a950c2fb65ec9034a
last_synced: '2026-04-23T10:17:20Z'
loc: 14
annotations: []
imports:
- pydantic
- .base
exports:
- DocBrief
- DocArtifact
tags:
- code
- python
coverage_pct: 100.0
---

# agents/contracts/documenter.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/documenter.py`](../../../agents/contracts/documenter.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from pydantic import Field

from .base import AgentInputBase, AgentOutputBase


class DocBrief(AgentInputBase):
    commit_hash: str
    scope: str
    files_touched: list[str]


class DocArtifact(AgentOutputBase):
    history_md_updated: bool
    codex_entries_created: list[str] = Field(default_factory=list)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `DocBrief` (class) — lines 6-9
- `DocArtifact` (class) — lines 12-14

### Imports
- `pydantic`
- `.base`

### Exports
- `DocBrief`
- `DocArtifact`

---
type: code-source
language: python
file_path: agents/contracts/coder.py
git_blob: 52e5ef762795742c861d065152d8ebc3b03f1552
last_synced: '2026-04-23T10:10:35Z'
loc: 21
annotations: []
imports:
- typing
- pydantic
- .base
exports:
- CodeBrief
- CodeArtifact
tags:
- code
- python
coverage_pct: 100.0
---

# agents/contracts/coder.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/coder.py`](../../../agents/contracts/coder.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from typing import Literal

from pydantic import Field

from .base import AgentInputBase, AgentOutputBase


class CodeBrief(AgentInputBase):
    spec_path: str
    target: Literal["backend", "android", "frontend"]
    target_files: list[str]
    constraints: list[str] = Field(default_factory=list)
    tests_red_path: str


class CodeArtifact(AgentOutputBase):
    files_modified: list[str]
    diff_path: str
    tests_green: bool
    test_output_path: str
    lint_clean: bool
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `CodeBrief` (class) — lines 8-13
- `CodeArtifact` (class) — lines 16-21

### Imports
- `typing`
- `pydantic`
- `.base`

### Exports
- `CodeBrief`
- `CodeArtifact`

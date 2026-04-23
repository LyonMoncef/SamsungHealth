---
type: code-source
language: python
file_path: agents/contracts/reviewer.py
git_blob: a3577143f12069bfa675a1500ab613a679888a5d
last_synced: '2026-04-23T10:17:20Z'
loc: 30
annotations: []
imports:
- typing
- pydantic
- .base
exports:
- ReviewBrief
- ReviewReport
tags:
- code
- python
coverage_pct: 100.0
---

# agents/contracts/reviewer.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/reviewer.py`](../../../agents/contracts/reviewer.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from typing import Literal

from pydantic import Field

from .base import AgentInputBase, AgentOutputBase


_DEFAULT_CHECKLIST = [
    "spec ↔ code alignment",
    "tests cover edge cases",
    "no secrets in code",
    "logging added for new events",
    "audit log written for RGPD-critical paths",
    "HISTORY.md updated",
]


class ReviewBrief(AgentInputBase):
    branch: str
    spec_path: str
    diff_path: str
    checklist: list[str] = Field(default_factory=lambda: list(_DEFAULT_CHECKLIST))


class ReviewReport(AgentOutputBase):
    findings: list[dict]
    overall: Literal["approve", "request_changes", "comment"]
    critical_count: int
    warning_count: int
    suggestion_count: int
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ReviewBrief` (class) — lines 18-22
- `ReviewReport` (class) — lines 25-30

### Imports
- `typing`
- `pydantic`
- `.base`

### Exports
- `ReviewBrief`
- `ReviewReport`

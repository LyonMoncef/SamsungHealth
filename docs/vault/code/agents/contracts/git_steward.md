---
type: code-source
language: python
file_path: agents/contracts/git_steward.py
git_blob: a5e72a976b3f881d3918f09c98c16c37f244f9f6
last_synced: '2026-04-23T08:13:16Z'
loc: 42
annotations: []
imports:
- typing
- pydantic
- .base
exports:
- GitOperationBrief
- GitOperationReport
tags:
- code
- python
---

# agents/contracts/git_steward.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/git_steward.py`](../../../agents/contracts/git_steward.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from typing import Literal

from pydantic import Field

from .base import AgentInputBase, AgentOutputBase


_AUTO_APPROVE_SAFE = [
    "git status",
    "git fetch",
    "git diff",
    "git log",
    "git ls-remote --tags",
    "gh pr view",
    "gh pr list",
]


class GitOperationBrief(AgentInputBase):
    op_type: Literal[
        "status",
        "commit",
        "tag",
        "checkpoint",
        "pr",
        "release",
        "fix",
        "audit_post_save",
    ]
    scope: str = ""
    dry_run: bool = False
    auto_approve_safe: list[str] = Field(default_factory=lambda: list(_AUTO_APPROVE_SAFE))
    files_changed: list[str] = Field(default_factory=list)


class GitOperationReport(AgentOutputBase):
    actions_taken: list[dict]
    actions_proposed: list[dict]
    warnings: list[str]
    requires_human_approval: bool
    history_md_updated: bool
    next_recommended: Literal["commit", "pr", "checkpoint", "fix", "none"] | None = None
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `GitOperationBrief` (class) — lines 19-33
- `GitOperationReport` (class) — lines 36-42

### Imports
- `typing`
- `pydantic`
- `.base`

### Exports
- `GitOperationBrief`
- `GitOperationReport`

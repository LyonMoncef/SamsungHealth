---
type: code-source
language: python
file_path: agents/contracts/base.py
git_blob: 6897748e5e959a1073c46f3657d7e0ab271760cb
last_synced: '2026-04-23T10:49:29Z'
loc: 41
annotations: []
imports:
- typing
- pydantic
exports:
- AgentInputBase
- AgentOutputBase
tags:
- code
- python
coverage_pct: 100.0
---

# agents/contracts/base.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/base.py`](../../../agents/contracts/base.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from typing import Literal

from pydantic import BaseModel, Field


NextRecommended = Literal[
    "spec",
    "tdd",
    "test",
    "impl",
    "review",
    "document",
    "commit",
    "pr",
    "merge",
    "checkpoint",
    "fix",
    "human",
    "release",
    "none",
]


class AgentInputBase(BaseModel):
    version: Literal["1.0"] = "1.0"
    task_id: str = Field(..., description="uuid ou timestamp-slug, sert de clé dir work/")
    invoked_by: str = Field(..., description="skill/agent/human qui déclenche")
    work_dir: str = Field(..., description="work/<task-id>/ path relatif repo")


class AgentOutputBase(BaseModel):
    version: Literal["1.0"] = "1.0"
    task_id: str
    agent: str = Field(..., description="nom de l'agent qui a produit")
    status: Literal["success", "partial", "failed", "needs_clarification"]
    summary: str = Field(..., max_length=500, description="<=500 chars, humain-lisible")
    artifacts: list[str] = Field(default_factory=list, description="file paths produits")
    tokens_used: int = 0
    duration_ms: int = 0
    next_recommended: NextRecommended | None = None
    blockers: list[str] = Field(default_factory=list, description="ce qui a bloqué si status != success")
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `AgentInputBase` (class) — lines 24-28
- `AgentOutputBase` (class) — lines 31-41

### Imports
- `typing`
- `pydantic`

### Exports
- `AgentInputBase`
- `AgentOutputBase`

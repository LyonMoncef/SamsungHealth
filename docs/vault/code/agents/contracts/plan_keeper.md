---
type: code-source
language: python
file_path: agents/contracts/plan_keeper.py
git_blob: 78c97f86db30354a584fa5ebb9602e6cb4213eef
last_synced: '2026-04-23T08:13:16Z'
loc: 42
annotations: []
imports:
- typing
- pydantic
- .base
- .pentester
exports:
- PlanAuditBrief
- PlanDeviation
- PlanAuditReport
tags:
- code
- python
---

# agents/contracts/plan_keeper.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/plan_keeper.py`](../../../agents/contracts/plan_keeper.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from typing import Literal

from pydantic import BaseModel, Field

from .base import AgentInputBase, AgentOutputBase
from .pentester import Severity


DeviationType = Literal[
    "agent_added_not_in_plan",
    "branch_naming_mismatch",
    "phase_scope_drift",
    "directory_structure_drift",
    "skill_added_not_in_plan",
    "file_orphan",
    "duration_estimate_drift",
    "other",
]


class PlanAuditBrief(AgentInputBase):
    triggered_by: Literal["skill", "agent", "commit", "manual", "post_delivery"]
    recent_changes: list[str] = Field(default_factory=list)
    plan_paths: list[str] = Field(default_factory=list)
    severity_threshold: Severity = "medium"


class PlanDeviation(BaseModel):
    deviation_type: DeviationType
    severity: Severity
    location: str
    plan_affected: str
    description: str
    proposed_patch: str


class PlanAuditReport(AgentOutputBase):
    deviations: list[PlanDeviation]
    coverage_gaps: list[str]
    plans_needing_update: list[str]
    overall: Literal["aligned", "drift_detected", "block"]
    next_recommended: Literal["commit", "impl", "spec", "none"] | None = None
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `PlanAuditBrief` (class) — lines 21-25
- `PlanDeviation` (class) — lines 28-34
- `PlanAuditReport` (class) — lines 37-42

### Imports
- `typing`
- `pydantic`
- `.base`
- `.pentester`

### Exports
- `PlanAuditBrief`
- `PlanDeviation`
- `PlanAuditReport`

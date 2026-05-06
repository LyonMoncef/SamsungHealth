---
type: code-source
language: python
file_path: agents/contracts/plan_keeper.py
git_blob: 024cb7ca8d021f46cd6eaae696aab5bbc13fa4b9
last_synced: '2026-05-06T08:02:33Z'
loc: 47
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
coverage_pct: 100.0
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
    "vault_orphan_annotation",
    "vault_missing_note",
    "vault_outdated",
    "spec_implements_drift",
    "untested_spec",
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

### Implements specs
- [[../../specs/2026-04-23-plan-specs-in-vault]] — symbols: `DeviationType`

### Symbols
- `PlanAuditBrief` (class) — lines 26-30
- `PlanDeviation` (class) — lines 33-39
- `PlanAuditReport` (class) — lines 42-47

### Imports
- `typing`
- `pydantic`
- `.base`
- `.pentester`

### Exports
- `PlanAuditBrief`
- `PlanDeviation`
- `PlanAuditReport`

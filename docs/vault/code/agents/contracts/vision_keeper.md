---
type: code-source
language: python
file_path: agents/contracts/vision_keeper.py
git_blob: 88aa2e6dc0ba2f0f40abbc9a4d3a72a01d79022f
last_synced: '2026-05-06T07:20:59Z'
loc: 25
annotations: []
imports:
- pydantic
- typing
exports:
- VisionAuditBrief
- DriftDetail
- VisionAuditResult
tags:
- code
- python
---

# agents/contracts/vision_keeper.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/vision_keeper.py`](../../../agents/contracts/vision_keeper.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from pydantic import BaseModel, Field
from typing import Literal


class VisionAuditBrief(BaseModel):
    artifact_path: str = Field(..., description="Path to the spec/plan/diff to audit")
    slug: str = Field(..., description="Short identifier for the work item")
    vision_path: str = Field(default="VISION.md", description="Path to VISION.md")
    work_dir: str = Field(default=".claude/work", description="Output directory for audit report")


class DriftDetail(BaseModel):
    principle_id: Literal["C1", "C2", "C3", "design", "llm", "semantic"]
    description: str
    severity: Literal["block", "warn", "info"]
    suggested_fix: str = ""


class VisionAuditResult(BaseModel):
    verdict: Literal["aligned", "drift_alert", "vision_update_needed"]
    score: int = Field(..., ge=0, le=100)
    slug: str
    drift_details: list[DriftDetail] = []
    patch_suggested: str = ""
    report_path: str = ""
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `VisionAuditBrief` (class) — lines 5-9
- `DriftDetail` (class) — lines 12-16
- `VisionAuditResult` (class) — lines 19-25

### Imports
- `pydantic`
- `typing`

### Exports
- `VisionAuditBrief`
- `DriftDetail`
- `VisionAuditResult`

---
type: code-source
language: python
file_path: agents/contracts/__init__.py
git_blob: d2d160cb5c1ca46f483bcd2c4226eba013802eee
last_synced: '2026-04-23T08:43:08Z'
loc: 65
annotations: []
imports:
- .base
- .cartographer
- .coder
- .documenter
- .git_steward
- .pentester
- .plan_keeper
- .reviewer
- .spec_writer
- .test_writer
exports: []
tags:
- code
- python
---

# agents/contracts/__init__.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/__init__.py`](../../../agents/contracts/__init__.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from .base import AgentInputBase, AgentOutputBase, NextRecommended
from .cartographer import (
    AnchorKind,
    AnchorLocation,
    Annotation,
    AnnotationOpBrief,
    AnnotationOpReport,
    CartographyBrief,
    CartographyReport,
)
from .coder import CodeArtifact, CodeBrief
from .documenter import DocArtifact, DocBrief
from .git_steward import GitOperationBrief, GitOperationReport
from .pentester import (
    FindingCategory,
    PentestBrief,
    PentestFinding,
    PentestReport,
    ScanMode,
    Severity,
)
from .plan_keeper import (
    DeviationType,
    PlanAuditBrief,
    PlanAuditReport,
    PlanDeviation,
)
from .reviewer import ReviewBrief, ReviewReport
from .spec_writer import SpecArtifact, SpecBrief
from .test_writer import TestArtifact, TestBrief

__all__ = [
    "AgentInputBase",
    "AgentOutputBase",
    "NextRecommended",
    "SpecBrief",
    "SpecArtifact",
    "TestBrief",
    "TestArtifact",
    "CodeBrief",
    "CodeArtifact",
    "ReviewBrief",
    "ReviewReport",
    "DocBrief",
    "DocArtifact",
    "GitOperationBrief",
    "GitOperationReport",
    "PentestBrief",
    "PentestFinding",
    "PentestReport",
    "ScanMode",
    "Severity",
    "FindingCategory",
    "PlanAuditBrief",
    "PlanAuditReport",
    "PlanDeviation",
    "DeviationType",
    "CartographyBrief",
    "CartographyReport",
    "AnnotationOpBrief",
    "AnnotationOpReport",
    "Annotation",
    "AnchorLocation",
    "AnchorKind",
]
```

---

## Appendix — symbols & navigation *(auto)*

### Imports
- `.base`
- `.cartographer`
- `.coder`
- `.documenter`
- `.git_steward`
- `.pentester`
- `.plan_keeper`
- `.reviewer`
- `.spec_writer`
- `.test_writer`

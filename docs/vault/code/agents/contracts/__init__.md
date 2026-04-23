---
type: code-source
language: python
file_path: agents/contracts/__init__.py
git_blob: ef3a05f66aba3dc17e8da5cdf00092dbfc9bce02
last_synced: '2026-04-23T10:17:20Z'
loc: 87
annotations: []
imports:
- .annotation_suggester
- .base
- .cartographer
- .coder
- .documenter
- .git_steward
- .pentester
- .plan_keeper
- .reviewer
- .spec
- .spec_writer
- .test_writer
exports: []
tags:
- code
- python
coverage_pct: 100.0
---

# agents/contracts/__init__.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/__init__.py`](../../../agents/contracts/__init__.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from .annotation_suggester import (
    AnnotationSuggestionBrief,
    AnnotationSuggestionReport,
    SuggestedAnnotation,
    SuggestionConfidence,
)
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
from .spec import (
    SpecImplements,
    SpecMeta,
    SpecStatus,
    SpecTestedBy,
    SpecType,
)
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
    "AnnotationSuggestionBrief",
    "AnnotationSuggestionReport",
    "SuggestedAnnotation",
    "SuggestionConfidence",
    "SpecMeta",
    "SpecImplements",
    "SpecTestedBy",
    "SpecType",
    "SpecStatus",
]
```

---

## Appendix — symbols & navigation *(auto)*

### Imports
- `.annotation_suggester`
- `.base`
- `.cartographer`
- `.coder`
- `.documenter`
- `.git_steward`
- `.pentester`
- `.plan_keeper`
- `.reviewer`
- `.spec`
- `.spec_writer`
- `.test_writer`

---
type: code-source
language: python
file_path: agents/contracts/cartographer.py
git_blob: eb5ff9e8f96627969fb1dee28e80c15be46b145a
last_synced: '2026-04-23T10:40:53Z'
loc: 67
annotations: []
imports:
- typing
- pydantic
- .base
exports:
- AnchorLocation
- Annotation
- CartographyBrief
- CartographyReport
- AnnotationOpBrief
- AnnotationOpReport
tags:
- code
- python
coverage_pct: 100.0
---

# agents/contracts/cartographer.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/cartographer.py`](../../../agents/contracts/cartographer.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from typing import Literal

from pydantic import BaseModel, Field

from .base import AgentInputBase, AgentOutputBase


AnchorKind = Literal["single", "range"]

SLUG_PATTERN = r"^[a-z0-9][a-z0-9-]{2,40}$"


class AnchorLocation(BaseModel):
    file: str
    kind: AnchorKind
    line: int | None = None
    begin_line: int | None = None
    end_line: int | None = None


class Annotation(BaseModel):
    slug: str = Field(pattern=SLUG_PATTERN)
    file_path: str
    anchors: list[AnchorLocation]
    scope: Literal["single-file", "cross-file"]
    status: Literal["active", "orphan", "archived"]
    created_by: Literal["human", "agent"]
    references: dict


class CartographyBrief(AgentInputBase):
    mode: Literal["full", "diff", "check"]
    languages: list[Literal["python", "javascript", "kotlin"]] = Field(
        default_factory=lambda: ["python", "javascript", "kotlin"]
    )
    diff_files: list[str] = Field(default_factory=list)
    detect_orphans: bool = True
    update_last_verified: bool = True


class CartographyReport(AgentOutputBase):
    notes_generated: int
    notes_updated: int
    notes_skipped: int
    annotations_processed: int
    new_orphans: list[str] = Field(default_factory=list)
    resolved_orphans: list[str] = Field(default_factory=list)
    parse_errors: list[dict] = Field(default_factory=list)
    overall: Literal["complete", "partial", "failed"]
    next_recommended: Literal["commit", "review", "anchor-review", "none"] | None = None


class AnnotationOpBrief(AgentInputBase):
    op: Literal["create", "update", "delete", "anchor-review"]
    slug: str = Field(pattern=SLUG_PATTERN)
    file_path: str | None = None
    target_line: int | None = None
    target_range: tuple[int, int] | None = None
    new_content: str | None = None


class AnnotationOpReport(AgentOutputBase):
    slug: str
    op_performed: str
    files_modified: list[str] = Field(default_factory=list)
    note_rerendered: bool
    overall: Literal["success", "needs_clarification", "failed"]
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `AnchorLocation` (class) — lines 13-18
- `Annotation` (class) — lines 21-28
- `CartographyBrief` (class) — lines 31-38
- `CartographyReport` (class) — lines 41-50
- `AnnotationOpBrief` (class) — lines 53-59
- `AnnotationOpReport` (class) — lines 62-67

### Imports
- `typing`
- `pydantic`
- `.base`

### Exports
- `AnchorLocation`
- `Annotation`
- `CartographyBrief`
- `CartographyReport`
- `AnnotationOpBrief`
- `AnnotationOpReport`

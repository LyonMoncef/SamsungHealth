---
type: code-source
language: python
file_path: agents/contracts/annotation_suggester.py
git_blob: 51784c63a759a728a1bb984e8e02d2bff9d7fb66
last_synced: '2026-04-23T09:43:48Z'
loc: 40
annotations: []
imports:
- typing
- pydantic
- .base
exports:
- AnnotationSuggestionBrief
- SuggestedAnnotation
- AnnotationSuggestionReport
tags:
- code
- python
coverage_pct: 100.0
---

# agents/contracts/annotation_suggester.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/annotation_suggester.py`](../../../agents/contracts/annotation_suggester.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from typing import Literal

from pydantic import BaseModel, Field

from .base import AgentInputBase, AgentOutputBase


SuggestionConfidence = Literal["low", "medium", "high"]


class AnnotationSuggestionBrief(AgentInputBase):
    triggered_by: Literal["post_commit", "manual", "skill"]
    commit_sha: str | None = None
    diff_files: list[str] = Field(default_factory=list)
    diff_text: str = ""
    commit_message: str = ""
    issue_refs: list[int] = Field(default_factory=list)
    pr_refs: list[int] = Field(default_factory=list)
    max_suggestions: int = 5
    confidence_threshold: SuggestionConfidence = "low"


class SuggestedAnnotation(BaseModel):
    slug: str = Field(pattern=r"^[a-z0-9][a-z0-9-]{2,40}$")
    file: str
    line: int | None = None
    begin_line: int | None = None
    end_line: int | None = None
    rationale: str
    body_draft: str
    confidence: SuggestionConfidence
    triggers: list[str] = Field(default_factory=list)


class AnnotationSuggestionReport(AgentOutputBase):
    suggestions: list[SuggestedAnnotation] = Field(default_factory=list)
    files_scanned: int = 0
    triggers_fired: list[str] = Field(default_factory=list)
    overall: Literal["suggestions_pending", "no_suggestion", "failed"]
    next_recommended: Literal["annotate", "commit", "none"] | None = None
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `AnnotationSuggestionBrief` (class) — lines 11-20
- `SuggestedAnnotation` (class) — lines 23-32
- `AnnotationSuggestionReport` (class) — lines 35-40

### Imports
- `typing`
- `pydantic`
- `.base`

### Exports
- `AnnotationSuggestionBrief`
- `SuggestedAnnotation`
- `AnnotationSuggestionReport`

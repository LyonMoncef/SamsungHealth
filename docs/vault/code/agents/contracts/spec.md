---
type: code-source
language: python
file_path: agents/contracts/spec.py
git_blob: d1c4b42a0c9c6fc58a1a6edf4bef3f3c40469561
last_synced: '2026-05-06T08:02:33Z'
loc: 53
annotations: []
imports:
- datetime
- typing
- pydantic
exports:
- SpecImplements
- SpecTestedBy
- SpecMeta
tags:
- code
- python
---

# agents/contracts/spec.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/spec.py`](../../../agents/contracts/spec.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Pydantic types for spec frontmatter (`docs/vault/specs/*.md`).

Specs are first-class vault notes that declare:
- `implements:` — list of file/symbols/line_range targets they describe
- `tested_by:` — list of test files/classes/methods that validate them
- `type:` — `spec | plan | us | feature | stub`
- `status:` — `draft | ready | in_progress | delivered | superseded`

The `spec_indexer` module reads these frontmatters and builds a global index
consumed by the `note_renderer` to surface bidirectional links between
specs ↔ code ↔ tests.
"""

from datetime import date
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


SpecType = Literal["spec", "plan", "us", "feature", "stub"]
SpecStatus = Literal[
    "draft", "ready", "approved", "in_progress",
    "delivered", "superseded", "reference",
]


class SpecImplements(BaseModel):
    file: str
    symbols: list[str] = Field(default_factory=list)
    line_range: tuple[int, int] | None = None


class SpecTestedBy(BaseModel):
    file: str
    classes: list[str] = Field(default_factory=list)
    methods: list[str] = Field(default_factory=list)


class SpecMeta(BaseModel):
    """Validates the frontmatter of a spec file."""

    model_config = ConfigDict(extra="ignore")  # tolerate extra YAML keys

    type: SpecType
    title: str | None = None
    slug: str | None = None  # derived from filename if absent
    status: SpecStatus = "draft"
    created: str | date | None = None
    delivered: str | date | None = None
    tags: list[str] = Field(default_factory=list)
    related_plans: list[str] = Field(default_factory=list)
    implements: list[SpecImplements] = Field(default_factory=list)
    tested_by: list[SpecTestedBy] = Field(default_factory=list)
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-23-plan-specs-in-vault]] — symbols: `SpecMeta`, `SpecImplements`, `SpecTestedBy`, `SpecType`, `SpecStatus`

### Symbols
- `SpecImplements` (class) — lines 27-30 · **Specs**: [[../../specs/2026-04-23-plan-specs-in-vault|2026-04-23-plan-specs-in-vault]]
- `SpecTestedBy` (class) — lines 33-36 · **Specs**: [[../../specs/2026-04-23-plan-specs-in-vault|2026-04-23-plan-specs-in-vault]]
- `SpecMeta` (class) — lines 39-53 · **Specs**: [[../../specs/2026-04-23-plan-specs-in-vault|2026-04-23-plan-specs-in-vault]]

### Imports
- `datetime`
- `typing`
- `pydantic`

### Exports
- `SpecImplements`
- `SpecTestedBy`
- `SpecMeta`

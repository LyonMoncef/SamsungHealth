---
type: code-source
language: python
file_path: agents/contracts/spec_writer.py
git_blob: 700e4b55cfaefa7107dc9a03a833773a78d1c7fb
last_synced: '2026-04-23T10:31:18Z'
loc: 20
annotations: []
imports:
- typing
- pydantic
- .base
exports:
- SpecBrief
- SpecArtifact
tags:
- code
- python
coverage_pct: 100.0
---

# agents/contracts/spec_writer.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/contracts/spec_writer.py`](../../../agents/contracts/spec_writer.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from typing import Literal

from pydantic import Field

from .base import AgentInputBase, AgentOutputBase


class SpecBrief(AgentInputBase):
    spec_type: Literal["module", "ui", "rgpd"]
    slug: str
    phase: Literal["P0", "P1", "P2", "P3", "P4", "P5", "P6"]
    context_files: list[str]
    parent_specs: list[str] = Field(default_factory=list)
    key_points: list[str]


class SpecArtifact(AgentOutputBase):
    spec_path: str
    sections_completed: list[str]
    issues_opened: list[int] = Field(default_factory=list)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SpecBrief` (class) — lines 8-14
- `SpecArtifact` (class) — lines 17-20

### Imports
- `typing`
- `pydantic`
- `.base`

### Exports
- `SpecBrief`
- `SpecArtifact`

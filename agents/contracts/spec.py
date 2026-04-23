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

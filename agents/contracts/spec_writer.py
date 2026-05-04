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

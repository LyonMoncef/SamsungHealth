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

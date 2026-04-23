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

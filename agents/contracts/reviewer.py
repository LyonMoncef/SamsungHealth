from typing import Literal

from pydantic import Field

from .base import AgentInputBase, AgentOutputBase


_DEFAULT_CHECKLIST = [
    "spec ↔ code alignment",
    "tests cover edge cases",
    "no secrets in code",
    "logging added for new events",
    "audit log written for RGPD-critical paths",
    "HISTORY.md updated",
]


class ReviewBrief(AgentInputBase):
    branch: str
    spec_path: str
    diff_path: str
    checklist: list[str] = Field(default_factory=lambda: list(_DEFAULT_CHECKLIST))


class ReviewReport(AgentOutputBase):
    findings: list[dict]
    overall: Literal["approve", "request_changes", "comment"]
    critical_count: int
    warning_count: int
    suggestion_count: int

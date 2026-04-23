from typing import Literal

from pydantic import Field

from .base import AgentInputBase, AgentOutputBase


_AUTO_APPROVE_SAFE = [
    "git status",
    "git fetch",
    "git diff",
    "git log",
    "git ls-remote --tags",
    "gh pr view",
    "gh pr list",
]


class GitOperationBrief(AgentInputBase):
    op_type: Literal[
        "status",
        "commit",
        "tag",
        "checkpoint",
        "pr",
        "release",
        "fix",
        "audit_post_save",
    ]
    scope: str = ""
    dry_run: bool = False
    auto_approve_safe: list[str] = Field(default_factory=lambda: list(_AUTO_APPROVE_SAFE))
    files_changed: list[str] = Field(default_factory=list)


class GitOperationReport(AgentOutputBase):
    actions_taken: list[dict]
    actions_proposed: list[dict]
    warnings: list[str]
    requires_human_approval: bool
    history_md_updated: bool
    next_recommended: Literal["commit", "pr", "checkpoint", "fix", "none"] | None = None

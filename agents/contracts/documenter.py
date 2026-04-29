from pydantic import Field

from .base import AgentInputBase, AgentOutputBase


class DocBrief(AgentInputBase):
    commit_hash: str
    scope: str
    files_touched: list[str]


class DocArtifact(AgentOutputBase):
    history_md_updated: bool
    codex_entries_created: list[str] = Field(default_factory=list)

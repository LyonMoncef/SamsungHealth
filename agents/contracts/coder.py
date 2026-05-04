from typing import Literal

from pydantic import Field

from .base import AgentInputBase, AgentOutputBase


class CodeBrief(AgentInputBase):
    spec_path: str
    target: Literal["backend", "android", "frontend"]
    target_files: list[str]
    constraints: list[str] = Field(default_factory=list)
    tests_red_path: str


class CodeArtifact(AgentOutputBase):
    files_modified: list[str]
    diff_path: str
    tests_green: bool
    test_output_path: str
    lint_clean: bool

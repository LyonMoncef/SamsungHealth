from .base import AgentInputBase, AgentOutputBase


class TestBrief(AgentInputBase):
    spec_path: str
    target_test_dir: str


class TestArtifact(AgentOutputBase):
    test_files: list[str]
    tests_red_count: int
    tests_green_count: int

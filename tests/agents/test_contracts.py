"""RED-first tests for agents/contracts/*.

Validates that every Pydantic contract :
- accepts a minimal valid payload
- rejects payloads missing required fields
- enforces Literal constraints (status, op_type, target, ...)
- enforces summary max_length=500
- defaults version to "1.0"
- exposes the right typed fields documented in plan v2-multi-agents-architecture.md
"""

import pytest
from pydantic import ValidationError


# ---------------------------------------------------------------------------
# AgentInputBase / AgentOutputBase
# ---------------------------------------------------------------------------

class TestAgentInputBase:
    def test_minimal_valid(self):
        from agents.contracts.base import AgentInputBase

        m = AgentInputBase(task_id="t-1", invoked_by="human", work_dir="work/t-1")
        assert m.version == "1.0"
        assert m.task_id == "t-1"
        assert m.invoked_by == "human"
        assert m.work_dir == "work/t-1"

    def test_rejects_missing_task_id(self):
        from agents.contracts.base import AgentInputBase

        with pytest.raises(ValidationError):
            AgentInputBase(invoked_by="human", work_dir="work/t-1")

    def test_rejects_missing_invoked_by(self):
        from agents.contracts.base import AgentInputBase

        with pytest.raises(ValidationError):
            AgentInputBase(task_id="t-1", work_dir="work/t-1")

    def test_rejects_missing_work_dir(self):
        from agents.contracts.base import AgentInputBase

        with pytest.raises(ValidationError):
            AgentInputBase(task_id="t-1", invoked_by="human")

    def test_version_must_be_one_zero(self):
        from agents.contracts.base import AgentInputBase

        with pytest.raises(ValidationError):
            AgentInputBase(version="2.0", task_id="t", invoked_by="h", work_dir="w")


class TestAgentOutputBase:
    def test_minimal_valid(self):
        from agents.contracts.base import AgentOutputBase

        m = AgentOutputBase(task_id="t-1", agent="spec-writer", status="success", summary="ok")
        assert m.version == "1.0"
        assert m.tokens_used == 0
        assert m.duration_ms == 0
        assert m.artifacts == []
        assert m.blockers == []
        assert m.next_recommended is None

    def test_status_literal_enforced(self):
        from agents.contracts.base import AgentOutputBase

        for s in ("success", "partial", "failed", "needs_clarification"):
            AgentOutputBase(task_id="t", agent="a", status=s, summary="s")
        with pytest.raises(ValidationError):
            AgentOutputBase(task_id="t", agent="a", status="weird", summary="s")

    def test_summary_max_500(self):
        from agents.contracts.base import AgentOutputBase

        AgentOutputBase(task_id="t", agent="a", status="success", summary="x" * 500)
        with pytest.raises(ValidationError):
            AgentOutputBase(task_id="t", agent="a", status="success", summary="x" * 501)

    def test_next_recommended_literal_accepts_known_values(self):
        from agents.contracts.base import AgentOutputBase

        for v in ("spec", "tdd", "test", "impl", "review", "document",
                  "commit", "pr", "merge", "checkpoint", "fix",
                  "human", "release", "none"):
            AgentOutputBase(task_id="t", agent="a", status="success", summary="s", next_recommended=v)

    def test_next_recommended_rejects_unknown(self):
        from agents.contracts.base import AgentOutputBase

        with pytest.raises(ValidationError):
            AgentOutputBase(task_id="t", agent="a", status="success", summary="s",
                            next_recommended="garbage")


# ---------------------------------------------------------------------------
# spec_writer
# ---------------------------------------------------------------------------

class TestSpecWriter:
    def test_brief_valid(self):
        from agents.contracts.spec_writer import SpecBrief

        b = SpecBrief(
            task_id="t", invoked_by="human", work_dir="work/t",
            spec_type="module", slug="p2-sleep", phase="P2",
            context_files=["server/routers/sleep.py"], key_points=["multi-user", "encryption"],
        )
        assert b.spec_type == "module"
        assert b.parent_specs == []

    def test_brief_rejects_bad_phase(self):
        from agents.contracts.spec_writer import SpecBrief

        with pytest.raises(ValidationError):
            SpecBrief(
                task_id="t", invoked_by="h", work_dir="w",
                spec_type="module", slug="x", phase="P9",
                context_files=[], key_points=["a"],
            )

    def test_brief_rejects_bad_spec_type(self):
        from agents.contracts.spec_writer import SpecBrief

        with pytest.raises(ValidationError):
            SpecBrief(
                task_id="t", invoked_by="h", work_dir="w",
                spec_type="weird", slug="x", phase="P0",
                context_files=[], key_points=["a"],
            )

    def test_artifact_valid(self):
        from agents.contracts.spec_writer import SpecArtifact

        a = SpecArtifact(
            task_id="t", agent="spec-writer", status="success", summary="spec ready",
            spec_path="vault/spec.md", sections_completed=["resp", "types"],
        )
        assert a.issues_opened == []


# ---------------------------------------------------------------------------
# test_writer
# ---------------------------------------------------------------------------

class TestTestWriter:
    def test_brief_valid(self):
        from agents.contracts.test_writer import TestBrief

        b = TestBrief(
            task_id="t", invoked_by="h", work_dir="w",
            spec_path="vault/spec.md", target_test_dir="tests/sleep/",
        )
        assert b.target_test_dir == "tests/sleep/"

    def test_brief_rejects_missing_spec(self):
        from agents.contracts.test_writer import TestBrief

        with pytest.raises(ValidationError):
            TestBrief(task_id="t", invoked_by="h", work_dir="w", target_test_dir="tests/")

    def test_artifact_valid(self):
        from agents.contracts.test_writer import TestArtifact

        a = TestArtifact(
            task_id="t", agent="test-writer", status="success", summary="12 RED",
            test_files=["tests/sleep/test_x.py"], tests_red_count=12, tests_green_count=0,
        )
        assert a.tests_red_count == 12


# ---------------------------------------------------------------------------
# coder
# ---------------------------------------------------------------------------

class TestCoder:
    def test_brief_valid_backend(self):
        from agents.contracts.coder import CodeBrief

        b = CodeBrief(
            task_id="t", invoked_by="h", work_dir="w",
            spec_path="vault/spec.md", target="backend",
            target_files=["server/routers/sleep.py"], tests_red_path="tests/sleep/",
        )
        assert b.target == "backend"
        assert b.constraints == []

    def test_brief_rejects_bad_target(self):
        from agents.contracts.coder import CodeBrief

        with pytest.raises(ValidationError):
            CodeBrief(
                task_id="t", invoked_by="h", work_dir="w",
                spec_path="s", target="kernel", target_files=["a"], tests_red_path="t",
            )

    def test_artifact_valid(self):
        from agents.contracts.coder import CodeArtifact

        a = CodeArtifact(
            task_id="t", agent="coder-backend", status="success", summary="green",
            files_modified=["server/x.py"], diff_path="work/t/diff.patch",
            tests_green=True, test_output_path="work/t/out.txt", lint_clean=True,
        )
        assert a.tests_green is True


# ---------------------------------------------------------------------------
# reviewer
# ---------------------------------------------------------------------------

class TestReviewer:
    def test_brief_valid_with_default_checklist(self):
        from agents.contracts.reviewer import ReviewBrief

        b = ReviewBrief(
            task_id="t", invoked_by="h", work_dir="w",
            branch="feat/x", spec_path="vault/s.md", diff_path="work/t/diff.patch",
        )
        assert "spec ↔ code alignment" in b.checklist
        assert len(b.checklist) >= 6

    def test_report_overall_literal(self):
        from agents.contracts.reviewer import ReviewReport

        for v in ("approve", "request_changes", "comment"):
            ReviewReport(
                task_id="t", agent="reviewer", status="success", summary="ok",
                findings=[], overall=v, critical_count=0, warning_count=0, suggestion_count=0,
            )
        with pytest.raises(ValidationError):
            ReviewReport(
                task_id="t", agent="reviewer", status="success", summary="ok",
                findings=[], overall="meh", critical_count=0, warning_count=0, suggestion_count=0,
            )


# ---------------------------------------------------------------------------
# documenter
# ---------------------------------------------------------------------------

class TestDocumenter:
    def test_brief_valid(self):
        from agents.contracts.documenter import DocBrief

        b = DocBrief(
            task_id="t", invoked_by="h", work_dir="w",
            commit_hash="abc1234", scope="phase-a-bootstrap", files_touched=["agents/README.md"],
        )
        assert b.commit_hash == "abc1234"

    def test_artifact_valid(self):
        from agents.contracts.documenter import DocArtifact

        a = DocArtifact(
            task_id="t", agent="documenter", status="success", summary="HISTORY updated",
            history_md_updated=True, codex_entries_created=["vault/codex/feature/123-x.md"],
        )
        assert a.history_md_updated is True
        assert len(a.codex_entries_created) == 1


# ---------------------------------------------------------------------------
# git_steward
# ---------------------------------------------------------------------------

class TestGitSteward:
    def test_brief_valid_status(self):
        from agents.contracts.git_steward import GitOperationBrief

        b = GitOperationBrief(
            task_id="t", invoked_by="h", work_dir="w",
            op_type="status",
        )
        assert b.dry_run is False
        assert "git status" in b.auto_approve_safe

    def test_brief_rejects_bad_op(self):
        from agents.contracts.git_steward import GitOperationBrief

        with pytest.raises(ValidationError):
            GitOperationBrief(task_id="t", invoked_by="h", work_dir="w", op_type="rebase_main")

    def test_brief_op_type_accepts_all_known(self):
        from agents.contracts.git_steward import GitOperationBrief

        for op in ("status", "commit", "tag", "checkpoint", "pr",
                   "release", "fix", "audit_post_save"):
            GitOperationBrief(task_id="t", invoked_by="h", work_dir="w", op_type=op)

    def test_report_valid(self):
        from agents.contracts.git_steward import GitOperationReport

        r = GitOperationReport(
            task_id="t", agent="git-steward", status="success", summary="committed",
            actions_taken=[{"cmd": "git commit", "exit_code": 0, "stdout_preview": "..."}],
            actions_proposed=[],
            warnings=[],
            requires_human_approval=False,
            history_md_updated=True,
            next_recommended="pr",
        )
        assert r.history_md_updated is True
        assert r.next_recommended == "pr"

    def test_report_next_recommended_literal(self):
        from agents.contracts.git_steward import GitOperationReport

        for v in ("commit", "pr", "checkpoint", "fix", "none"):
            GitOperationReport(
                task_id="t", agent="git-steward", status="success", summary="s",
                actions_taken=[], actions_proposed=[], warnings=[],
                requires_human_approval=False, history_md_updated=False,
                next_recommended=v,
            )
        with pytest.raises(ValidationError):
            GitOperationReport(
                task_id="t", agent="git-steward", status="success", summary="s",
                actions_taken=[], actions_proposed=[], warnings=[],
                requires_human_approval=False, history_md_updated=False,
                next_recommended="merge",
            )


# ---------------------------------------------------------------------------
# Cross-cutting : __init__ re-exports
# ---------------------------------------------------------------------------

class TestPackageReExports:
    def test_imports_from_agents_contracts(self):
        from agents.contracts import (
            AgentInputBase,
            AgentOutputBase,
            SpecBrief,
            SpecArtifact,
            TestBrief,
            TestArtifact,
            CodeBrief,
            CodeArtifact,
            ReviewBrief,
            ReviewReport,
            DocBrief,
            DocArtifact,
            GitOperationBrief,
            GitOperationReport,
        )

        assert AgentInputBase is not None
        assert GitOperationReport is not None

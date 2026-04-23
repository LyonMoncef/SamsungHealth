---
type: code-source
language: python
file_path: tests/agents/test_contracts.py
git_blob: 499d2620f0f19cce7ed7808d1509d1ae6e6825f8
last_synced: '2026-04-23T10:21:39Z'
loc: 1099
annotations: []
imports:
- pytest
- pydantic
exports:
- TestAgentInputBase
- TestAgentOutputBase
- TestSpecWriter
- TestTestWriter
- TestCoder
- TestReviewer
- TestDocumenter
- TestGitSteward
- TestPentester
- TestPlanKeeper
- TestCartographer
- TestAnnotationSuggester
- TestSpec
- TestPackageReExports
tags:
- code
- python
---

# tests/agents/test_contracts.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/agents/test_contracts.py`](../../../tests/agents/test_contracts.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
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
# pentester
# ---------------------------------------------------------------------------

class TestPentester:
    def test_brief_minimal_valid(self):
        from agents.contracts.pentester import PentestBrief

        b = PentestBrief(
            task_id="t", invoked_by="h", work_dir="w",
            branch="chore/x",
        )
        assert b.scope_mode == "full_repo"
        assert b.severity_threshold == "medium"
        assert b.allow_poc_generation is True
        assert b.allow_poc_execution is False
        assert b.quick is False
        assert "sast" in b.scan_modes
        assert "dast" not in b.scan_modes  # not in default

    def test_brief_scope_mode_literal(self):
        from agents.contracts.pentester import PentestBrief

        for sm in ("full_repo", "diff_only", "files_only"):
            PentestBrief(task_id="t", invoked_by="h", work_dir="w", branch="b", scope_mode=sm)
        with pytest.raises(ValidationError):
            PentestBrief(task_id="t", invoked_by="h", work_dir="w", branch="b", scope_mode="weird")

    def test_brief_severity_threshold_literal(self):
        from agents.contracts.pentester import PentestBrief

        for sev in ("info", "low", "medium", "high", "critical"):
            PentestBrief(task_id="t", invoked_by="h", work_dir="w", branch="b", severity_threshold=sev)
        with pytest.raises(ValidationError):
            PentestBrief(task_id="t", invoked_by="h", work_dir="w", branch="b", severity_threshold="extreme")

    def test_brief_scan_modes_literal_items(self):
        from agents.contracts.pentester import PentestBrief

        PentestBrief(
            task_id="t", invoked_by="h", work_dir="w", branch="b",
            scan_modes=["sast", "sca", "secrets", "dast", "semantic", "rgpd"],
        )
        with pytest.raises(ValidationError):
            PentestBrief(
                task_id="t", invoked_by="h", work_dir="w", branch="b",
                scan_modes=["telepathic"],
            )

    def test_finding_valid(self):
        from agents.contracts.pentester import PentestFinding

        f = PentestFinding(
            severity="high",
            category="injection",
            cwe="CWE-89",
            owasp="A03:2021",
            location="server/routers/sleep.py:42",
            description="Raw SQL with user input",
            exploit_scenario="curl ?id=1' OR '1'='1",
            remediation="Use parametrized queries",
            references=["https://owasp.org/Top10/A03_2021-Injection/"],
        )
        assert f.severity == "high"
        assert f.proof_of_concept_path is None  # default

    def test_finding_severity_literal(self):
        from agents.contracts.pentester import PentestFinding

        for s in ("critical", "high", "medium", "low", "info"):
            PentestFinding(
                severity=s, category="other", location="x", description="d",
                exploit_scenario="e", remediation="r",
            )
        with pytest.raises(ValidationError):
            PentestFinding(
                severity="apocalyptic", category="other", location="x", description="d",
                exploit_scenario="e", remediation="r",
            )

    def test_finding_category_literal(self):
        from agents.contracts.pentester import PentestFinding

        for c in ("injection", "auth", "crypto", "secrets", "rgpd",
                  "deps", "config", "logic", "ssrf", "xss", "other"):
            PentestFinding(
                severity="low", category=c, location="x", description="d",
                exploit_scenario="e", remediation="r",
            )
        with pytest.raises(ValidationError):
            PentestFinding(
                severity="low", category="vibes", location="x", description="d",
                exploit_scenario="e", remediation="r",
            )

    def test_report_valid(self):
        from agents.contracts.pentester import PentestReport, PentestFinding

        f = PentestFinding(
            severity="medium", category="auth", location="server/x.py:1",
            description="d", exploit_scenario="e", remediation="r",
        )
        r = PentestReport(
            task_id="t", agent="pentester", status="success", summary="1 medium found",
            findings=[f],
            scan_modes_run=["sast", "secrets"],
            tools_used=["bandit", "gitleaks"],
            critical_count=0, high_count=0, medium_count=1, low_count=0, info_count=0,
            overall="warn",
            next_recommended="commit",
        )
        assert r.findings[0].category == "auth"
        assert r.overall == "warn"

    def test_report_overall_literal(self):
        from agents.contracts.pentester import PentestReport

        for v in ("pass", "block_merge", "warn"):
            PentestReport(
                task_id="t", agent="pentester", status="success", summary="s",
                findings=[], scan_modes_run=[], tools_used=[],
                critical_count=0, high_count=0, medium_count=0, low_count=0, info_count=0,
                overall=v,
            )
        with pytest.raises(ValidationError):
            PentestReport(
                task_id="t", agent="pentester", status="success", summary="s",
                findings=[], scan_modes_run=[], tools_used=[],
                critical_count=0, high_count=0, medium_count=0, low_count=0, info_count=0,
                overall="meh",
            )

    def test_report_next_recommended_literal_restricted(self):
        from agents.contracts.pentester import PentestReport

        for v in ("impl", "review", "commit", "none"):
            PentestReport(
                task_id="t", agent="pentester", status="success", summary="s",
                findings=[], scan_modes_run=[], tools_used=[],
                critical_count=0, high_count=0, medium_count=0, low_count=0, info_count=0,
                overall="pass",
                next_recommended=v,
            )
        with pytest.raises(ValidationError):
            PentestReport(
                task_id="t", agent="pentester", status="success", summary="s",
                findings=[], scan_modes_run=[], tools_used=[],
                critical_count=0, high_count=0, medium_count=0, low_count=0, info_count=0,
                overall="pass",
                next_recommended="merge",  # not in pentester's allowed nexts
            )


# ---------------------------------------------------------------------------
# plan_keeper
# ---------------------------------------------------------------------------

class TestPlanKeeper:
    def test_brief_minimal_valid(self):
        from agents.contracts.plan_keeper import PlanAuditBrief

        b = PlanAuditBrief(
            task_id="t", invoked_by="h", work_dir="w",
            triggered_by="commit",
        )
        assert b.severity_threshold == "medium"
        assert b.recent_changes == []
        assert b.plan_paths == []  # default = auto-discover

    def test_brief_triggered_by_literal(self):
        from agents.contracts.plan_keeper import PlanAuditBrief

        for t in ("skill", "agent", "commit", "manual", "post_delivery"):
            PlanAuditBrief(task_id="t", invoked_by="h", work_dir="w", triggered_by=t)
        with pytest.raises(ValidationError):
            PlanAuditBrief(task_id="t", invoked_by="h", work_dir="w", triggered_by="cosmic")

    def test_brief_severity_threshold_literal(self):
        from agents.contracts.plan_keeper import PlanAuditBrief

        for sev in ("info", "low", "medium", "high", "critical"):
            PlanAuditBrief(
                task_id="t", invoked_by="h", work_dir="w",
                triggered_by="manual", severity_threshold=sev,
            )
        with pytest.raises(ValidationError):
            PlanAuditBrief(
                task_id="t", invoked_by="h", work_dir="w",
                triggered_by="manual", severity_threshold="apocalyptic",
            )

    def test_deviation_valid(self):
        from agents.contracts.plan_keeper import PlanDeviation

        d = PlanDeviation(
            deviation_type="agent_added_not_in_plan",
            severity="medium",
            location="agents/contracts/plan_keeper.py",
            plan_affected="vault/.../plan-v2-multi-agents-architecture.md",
            description="Agent plan-keeper créé sans entry dans inventaire",
            proposed_patch="Ajouter ligne dans table §Inventaire des agents groupe 1",
        )
        assert d.deviation_type == "agent_added_not_in_plan"

    def test_deviation_type_literal(self):
        from agents.contracts.plan_keeper import PlanDeviation

        for t in (
            "agent_added_not_in_plan",
            "branch_naming_mismatch",
            "phase_scope_drift",
            "directory_structure_drift",
            "skill_added_not_in_plan",
            "file_orphan",
            "duration_estimate_drift",
            "vault_orphan_annotation",
            "vault_missing_note",
            "vault_outdated",
            "spec_implements_drift",
            "untested_spec",
            "other",
        ):
            PlanDeviation(
                deviation_type=t, severity="low", location="x",
                plan_affected="p.md", description="d", proposed_patch="p",
            )
        with pytest.raises(ValidationError):
            PlanDeviation(
                deviation_type="vibes", severity="low", location="x",
                plan_affected="p.md", description="d", proposed_patch="p",
            )

    def test_report_valid(self):
        from agents.contracts.plan_keeper import PlanAuditReport, PlanDeviation

        d = PlanDeviation(
            deviation_type="branch_naming_mismatch", severity="low",
            location=".githooks/pre-push", plan_affected="vault/x.md",
            description="d", proposed_patch="p",
        )
        r = PlanAuditReport(
            task_id="t", agent="plan-keeper", status="success", summary="1 deviation",
            deviations=[d],
            coverage_gaps=["agents/contracts/plan_keeper.py"],
            plans_needing_update=["vault/x.md"],
            overall="drift_detected",
            next_recommended="commit",
        )
        assert r.overall == "drift_detected"
        assert len(r.deviations) == 1

    def test_report_overall_literal(self):
        from agents.contracts.plan_keeper import PlanAuditReport

        for v in ("aligned", "drift_detected", "block"):
            PlanAuditReport(
                task_id="t", agent="plan-keeper", status="success", summary="s",
                deviations=[], coverage_gaps=[], plans_needing_update=[],
                overall=v,
            )
        with pytest.raises(ValidationError):
            PlanAuditReport(
                task_id="t", agent="plan-keeper", status="success", summary="s",
                deviations=[], coverage_gaps=[], plans_needing_update=[],
                overall="meh",
            )

    def test_report_next_recommended_literal_restricted(self):
        from agents.contracts.plan_keeper import PlanAuditReport

        for v in ("commit", "impl", "spec", "none"):
            PlanAuditReport(
                task_id="t", agent="plan-keeper", status="success", summary="s",
                deviations=[], coverage_gaps=[], plans_needing_update=[],
                overall="aligned", next_recommended=v,
            )
        with pytest.raises(ValidationError):
            PlanAuditReport(
                task_id="t", agent="plan-keeper", status="success", summary="s",
                deviations=[], coverage_gaps=[], plans_needing_update=[],
                overall="aligned", next_recommended="merge",  # not in plan-keeper allowed
            )


# ---------------------------------------------------------------------------
# cartographer (Phase A.5 — code-as-vault)
# ---------------------------------------------------------------------------

class TestCartographer:
    # --- AnchorLocation ---------------------------------------------------

    def test_anchor_single_valid(self):
        from agents.contracts.cartographer import AnchorLocation

        a = AnchorLocation(file="server/routers/sleep.py", kind="single", line=21)
        assert a.kind == "single"
        assert a.line == 21
        assert a.begin_line is None
        assert a.end_line is None

    def test_anchor_range_valid(self):
        from agents.contracts.cartographer import AnchorLocation

        a = AnchorLocation(
            file="server/routers/sleep.py", kind="range",
            begin_line=18, end_line=30,
        )
        assert a.kind == "range"
        assert a.begin_line == 18
        assert a.end_line == 30
        assert a.line is None

    def test_anchor_kind_literal_enforced(self):
        from agents.contracts.cartographer import AnchorLocation

        with pytest.raises(ValidationError):
            AnchorLocation(file="x.py", kind="weird", line=1)

    # --- Annotation -------------------------------------------------------

    def test_annotation_minimal_valid(self):
        from agents.contracts.cartographer import Annotation, AnchorLocation

        ann = Annotation(
            slug="sleep-perf-cap",
            file_path="docs/vault/annotations/server/routers/sleep/sleep-perf-cap.md",
            anchors=[AnchorLocation(file="server/routers/sleep.py", kind="single", line=21)],
            scope="single-file",
            status="active",
            created_by="human",
            references={"issue": 142},
        )
        assert ann.slug == "sleep-perf-cap"
        assert ann.scope == "single-file"

    def test_annotation_slug_pattern_enforced(self):
        from agents.contracts.cartographer import Annotation, AnchorLocation

        anchors = [AnchorLocation(file="x.py", kind="single", line=1)]
        common = dict(
            file_path="docs/vault/annotations/x.md",
            anchors=anchors, scope="single-file", status="active",
            created_by="human", references={},
        )

        for good in ("sleep-perf-cap", "n1-query-risk", "abc", "a1b", "x" * 41):
            Annotation(slug=good, **common)

        for bad in ("Sleep_Perf", "x", "_temp", "ab", "x" * 42, "UPPER", "with space", "kebab--ok"):
            if bad == "kebab--ok":
                # double-dash is allowed by the regex; skip false positive
                continue
            with pytest.raises(ValidationError):
                Annotation(slug=bad, **common)

    def test_annotation_scope_literal(self):
        from agents.contracts.cartographer import Annotation, AnchorLocation

        anchors = [AnchorLocation(file="x.py", kind="single", line=1)]
        common = dict(
            slug="abc-def", file_path="x.md", anchors=anchors,
            status="active", created_by="human", references={},
        )
        for s in ("single-file", "cross-file"):
            Annotation(scope=s, **common)
        with pytest.raises(ValidationError):
            Annotation(scope="multi-galaxy", **common)

    def test_annotation_status_literal(self):
        from agents.contracts.cartographer import Annotation, AnchorLocation

        anchors = [AnchorLocation(file="x.py", kind="single", line=1)]
        common = dict(
            slug="abc-def", file_path="x.md", anchors=anchors,
            scope="single-file", created_by="human", references={},
        )
        for s in ("active", "orphan", "archived"):
            Annotation(status=s, **common)
        with pytest.raises(ValidationError):
            Annotation(status="ghost", **common)

    def test_annotation_created_by_literal(self):
        from agents.contracts.cartographer import Annotation, AnchorLocation

        anchors = [AnchorLocation(file="x.py", kind="single", line=1)]
        common = dict(
            slug="abc-def", file_path="x.md", anchors=anchors,
            scope="single-file", status="active", references={},
        )
        for s in ("human", "agent"):
            Annotation(created_by=s, **common)
        with pytest.raises(ValidationError):
            Annotation(created_by="alien", **common)

    # --- CartographyBrief -------------------------------------------------

    def test_cartography_brief_minimal_valid(self):
        from agents.contracts.cartographer import CartographyBrief

        b = CartographyBrief(
            task_id="t", invoked_by="h", work_dir="w",
            mode="full",
        )
        assert b.mode == "full"
        assert b.languages == ["python", "javascript", "kotlin"]
        assert b.diff_files == []
        assert b.detect_orphans is True
        assert b.update_last_verified is True

    def test_cartography_brief_mode_literal(self):
        from agents.contracts.cartographer import CartographyBrief

        for m in ("full", "diff", "check"):
            CartographyBrief(task_id="t", invoked_by="h", work_dir="w", mode=m)
        with pytest.raises(ValidationError):
            CartographyBrief(task_id="t", invoked_by="h", work_dir="w", mode="partial")

    def test_cartography_brief_languages_literal(self):
        from agents.contracts.cartographer import CartographyBrief

        CartographyBrief(
            task_id="t", invoked_by="h", work_dir="w",
            mode="full", languages=["python"],
        )
        with pytest.raises(ValidationError):
            CartographyBrief(
                task_id="t", invoked_by="h", work_dir="w",
                mode="full", languages=["cobol"],
            )

    # --- CartographyReport ------------------------------------------------

    def test_cartography_report_valid(self):
        from agents.contracts.cartographer import CartographyReport

        r = CartographyReport(
            task_id="t", agent="code-cartographer", status="success", summary="ok",
            notes_generated=34, notes_updated=0, notes_skipped=0,
            annotations_processed=0,
            new_orphans=[], resolved_orphans=[],
            parse_errors=[],
            overall="complete",
        )
        assert r.overall == "complete"
        assert r.notes_generated == 34

    def test_cartography_report_overall_literal(self):
        from agents.contracts.cartographer import CartographyReport

        for v in ("complete", "partial", "failed"):
            CartographyReport(
                task_id="t", agent="code-cartographer", status="success", summary="s",
                notes_generated=0, notes_updated=0, notes_skipped=0,
                annotations_processed=0,
                new_orphans=[], resolved_orphans=[], parse_errors=[],
                overall=v,
            )
        with pytest.raises(ValidationError):
            CartographyReport(
                task_id="t", agent="code-cartographer", status="success", summary="s",
                notes_generated=0, notes_updated=0, notes_skipped=0,
                annotations_processed=0,
                new_orphans=[], resolved_orphans=[], parse_errors=[],
                overall="meh",
            )

    def test_cartography_report_next_recommended_literal_restricted(self):
        from agents.contracts.cartographer import CartographyReport

        for v in ("commit", "review", "anchor-review", "none"):
            CartographyReport(
                task_id="t", agent="code-cartographer", status="success", summary="s",
                notes_generated=0, notes_updated=0, notes_skipped=0,
                annotations_processed=0,
                new_orphans=[], resolved_orphans=[], parse_errors=[],
                overall="complete", next_recommended=v,
            )
        with pytest.raises(ValidationError):
            CartographyReport(
                task_id="t", agent="code-cartographer", status="success", summary="s",
                notes_generated=0, notes_updated=0, notes_skipped=0,
                annotations_processed=0,
                new_orphans=[], resolved_orphans=[], parse_errors=[],
                overall="complete", next_recommended="merge",
            )

    # --- AnnotationOpBrief ------------------------------------------------

    def test_annotation_op_brief_create_valid(self):
        from agents.contracts.cartographer import AnnotationOpBrief

        b = AnnotationOpBrief(
            task_id="t", invoked_by="h", work_dir="w",
            op="create", slug="sleep-perf-cap",
            file_path="server/routers/sleep.py", target_line=21,
        )
        assert b.op == "create"
        assert b.slug == "sleep-perf-cap"
        assert b.target_line == 21

    def test_annotation_op_brief_op_literal(self):
        from agents.contracts.cartographer import AnnotationOpBrief

        for op in ("create", "update", "delete", "anchor-review"):
            AnnotationOpBrief(
                task_id="t", invoked_by="h", work_dir="w",
                op=op, slug="abc-def",
            )
        with pytest.raises(ValidationError):
            AnnotationOpBrief(
                task_id="t", invoked_by="h", work_dir="w",
                op="reincarnate", slug="abc-def",
            )

    def test_annotation_op_brief_slug_pattern(self):
        from agents.contracts.cartographer import AnnotationOpBrief

        with pytest.raises(ValidationError):
            AnnotationOpBrief(
                task_id="t", invoked_by="h", work_dir="w",
                op="create", slug="BadSlug",
            )

    def test_annotation_op_brief_range_pair(self):
        from agents.contracts.cartographer import AnnotationOpBrief

        b = AnnotationOpBrief(
            task_id="t", invoked_by="h", work_dir="w",
            op="create", slug="sleep-pipeline",
            file_path="server/routers/sleep.py", target_range=(18, 30),
        )
        assert b.target_range == (18, 30)

    # --- AnnotationOpReport -----------------------------------------------

    def test_annotation_op_report_valid(self):
        from agents.contracts.cartographer import AnnotationOpReport

        r = AnnotationOpReport(
            task_id="t", agent="code-cartographer", status="success", summary="created",
            slug="sleep-perf-cap", op_performed="create",
            files_modified=[
                "server/routers/sleep.py",
                "docs/vault/annotations/server/routers/sleep/sleep-perf-cap.md",
            ],
            note_rerendered=True,
            overall="success",
        )
        assert r.note_rerendered is True
        assert r.overall == "success"

    def test_annotation_op_report_overall_literal(self):
        from agents.contracts.cartographer import AnnotationOpReport

        for v in ("success", "needs_clarification", "failed"):
            AnnotationOpReport(
                task_id="t", agent="code-cartographer", status="success", summary="s",
                slug="abc-def", op_performed="create",
                files_modified=[], note_rerendered=False,
                overall=v,
            )
        with pytest.raises(ValidationError):
            AnnotationOpReport(
                task_id="t", agent="code-cartographer", status="success", summary="s",
                slug="abc-def", op_performed="create",
                files_modified=[], note_rerendered=False,
                overall="probably",
            )


# ---------------------------------------------------------------------------
# annotation_suggester (Phase A.6)
# ---------------------------------------------------------------------------

class TestAnnotationSuggester:
    def test_brief_minimal_valid(self):
        from agents.contracts.annotation_suggester import AnnotationSuggestionBrief

        b = AnnotationSuggestionBrief(
            task_id="t", invoked_by="hook", work_dir="w",
            triggered_by="post_commit",
        )
        assert b.commit_sha is None
        assert b.diff_files == []
        assert b.max_suggestions == 5
        assert b.confidence_threshold == "low"

    def test_brief_triggered_by_literal(self):
        from agents.contracts.annotation_suggester import AnnotationSuggestionBrief

        for t in ("post_commit", "manual", "skill"):
            AnnotationSuggestionBrief(
                task_id="t", invoked_by="h", work_dir="w", triggered_by=t,
            )
        with pytest.raises(ValidationError):
            AnnotationSuggestionBrief(
                task_id="t", invoked_by="h", work_dir="w", triggered_by="cron",
            )

    def test_suggested_annotation_valid(self):
        from agents.contracts.annotation_suggester import SuggestedAnnotation

        s = SuggestedAnnotation(
            slug="perf-cap",
            file="server/x.py",
            line=21,
            rationale="Commit message mentions perf cap",
            body_draft="# Why",
            confidence="high",
            triggers=["keyword:perf", "issue_ref:#142"],
        )
        assert s.slug == "perf-cap"
        assert s.confidence == "high"

    def test_suggested_slug_pattern(self):
        from agents.contracts.annotation_suggester import SuggestedAnnotation

        with pytest.raises(ValidationError):
            SuggestedAnnotation(
                slug="BadSlug", file="x.py", line=1,
                rationale="r", body_draft="b", confidence="low",
            )

    def test_suggested_confidence_literal(self):
        from agents.contracts.annotation_suggester import SuggestedAnnotation

        for c in ("low", "medium", "high"):
            SuggestedAnnotation(
                slug="abc-def", file="x.py", line=1,
                rationale="r", body_draft="b", confidence=c,
            )
        with pytest.raises(ValidationError):
            SuggestedAnnotation(
                slug="abc-def", file="x.py", line=1,
                rationale="r", body_draft="b", confidence="meh",
            )

    def test_report_overall_literal(self):
        from agents.contracts.annotation_suggester import AnnotationSuggestionReport

        for v in ("suggestions_pending", "no_suggestion", "failed"):
            AnnotationSuggestionReport(
                task_id="t", agent="annotation-suggester",
                status="success", summary="s",
                suggestions=[], files_scanned=0, triggers_fired=[],
                overall=v,
            )
        with pytest.raises(ValidationError):
            AnnotationSuggestionReport(
                task_id="t", agent="annotation-suggester",
                status="success", summary="s",
                suggestions=[], files_scanned=0, triggers_fired=[],
                overall="??",
            )

    def test_report_next_recommended_literal_restricted(self):
        from agents.contracts.annotation_suggester import AnnotationSuggestionReport

        for v in ("annotate", "commit", "none"):
            AnnotationSuggestionReport(
                task_id="t", agent="annotation-suggester",
                status="success", summary="s",
                suggestions=[], files_scanned=0, triggers_fired=[],
                overall="no_suggestion", next_recommended=v,
            )
        with pytest.raises(ValidationError):
            AnnotationSuggestionReport(
                task_id="t", agent="annotation-suggester",
                status="success", summary="s",
                suggestions=[], files_scanned=0, triggers_fired=[],
                overall="no_suggestion", next_recommended="merge",
            )


# ---------------------------------------------------------------------------
# spec (Phase A.8) — frontmatter typed
# ---------------------------------------------------------------------------

class TestSpec:
    def test_meta_minimal_valid(self):
        from agents.contracts.spec import SpecMeta

        m = SpecMeta(type="spec")
        assert m.status == "draft"
        assert m.implements == []
        assert m.tested_by == []

    def test_type_literal(self):
        from agents.contracts.spec import SpecMeta

        for t in ("spec", "plan", "us", "feature", "stub"):
            SpecMeta(type=t)
        with pytest.raises(ValidationError):
            SpecMeta(type="random")

    def test_status_literal(self):
        from agents.contracts.spec import SpecMeta

        for s in ("draft", "ready", "approved", "in_progress", "delivered", "superseded"):
            SpecMeta(type="spec", status=s)
        with pytest.raises(ValidationError):
            SpecMeta(type="spec", status="randomvalue")  # not in literal

    def test_implements_with_symbols_and_range(self):
        from agents.contracts.spec import SpecImplements, SpecMeta

        m = SpecMeta(type="spec", implements=[
            SpecImplements(file="x.py", symbols=["foo"], line_range=(10, 50)),
        ])
        assert m.implements[0].line_range == (10, 50)

    def test_tested_by_classes_and_methods(self):
        from agents.contracts.spec import SpecMeta, SpecTestedBy

        m = SpecMeta(type="spec", tested_by=[
            SpecTestedBy(file="t.py", classes=["TestX"], methods=["test_y"]),
        ])
        assert m.tested_by[0].classes == ["TestX"]


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
            PentestBrief,
            PentestFinding,
            PentestReport,
            PlanAuditBrief,
            PlanAuditReport,
            PlanDeviation,
            CartographyBrief,
            CartographyReport,
            AnnotationOpBrief,
            AnnotationOpReport,
            Annotation,
            AnchorLocation,
            AnchorKind,
            AnnotationSuggestionBrief,
            AnnotationSuggestionReport,
            SuggestedAnnotation,
            SuggestionConfidence,
            SpecMeta,
            SpecImplements,
            SpecTestedBy,
            SpecType,
            SpecStatus,
        )

        assert AgentInputBase is not None
        assert GitOperationReport is not None
        assert PentestReport is not None
        assert CartographyReport is not None
        assert AnchorKind is not None
        assert AnnotationSuggestionReport is not None
        assert SuggestionConfidence is not None
        assert PlanAuditReport is not None
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestAgentInputBase` (class) — lines 20-52
- `TestAgentOutputBase` (class) — lines 55-95
- `TestSpecWriter` (class) — lines 102-141
- `TestTestWriter` (class) — lines 148-171
- `TestCoder` (class) — lines 178-207
- `TestReviewer` (class) — lines 214-237
- `TestDocumenter` (class) — lines 244-262
- `TestGitSteward` (class) — lines 269-324
- `TestPentester` (class) — lines 331-477
- `TestPlanKeeper` (class) — lines 484-608
- `TestCartographer` (class) — lines 615-894
- `TestAnnotationSuggester` (class) — lines 901-998
- `TestSpec` (class) — lines 1005-1044
- `TestPackageReExports` (class) — lines 1051-1099

### Imports
- `pytest`
- `pydantic`

### Exports
- `TestAgentInputBase`
- `TestAgentOutputBase`
- `TestSpecWriter`
- `TestTestWriter`
- `TestCoder`
- `TestReviewer`
- `TestDocumenter`
- `TestGitSteward`
- `TestPentester`
- `TestPlanKeeper`
- `TestCartographer`
- `TestAnnotationSuggester`
- `TestSpec`
- `TestPackageReExports`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-23-plan-specs-in-vault]] — classes: `TestSpec`, `TestPlanKeeper`, `TestPackageReExports`

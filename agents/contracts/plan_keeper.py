from typing import Literal

from pydantic import BaseModel, Field

from .base import AgentInputBase, AgentOutputBase
from .pentester import Severity


DeviationType = Literal[
    "agent_added_not_in_plan",
    "branch_naming_mismatch",
    "phase_scope_drift",
    "directory_structure_drift",
    "skill_added_not_in_plan",
    "file_orphan",
    "duration_estimate_drift",
    "other",
]


class PlanAuditBrief(AgentInputBase):
    triggered_by: Literal["skill", "agent", "commit", "manual", "post_delivery"]
    recent_changes: list[str] = Field(default_factory=list)
    plan_paths: list[str] = Field(default_factory=list)
    severity_threshold: Severity = "medium"


class PlanDeviation(BaseModel):
    deviation_type: DeviationType
    severity: Severity
    location: str
    plan_affected: str
    description: str
    proposed_patch: str


class PlanAuditReport(AgentOutputBase):
    deviations: list[PlanDeviation]
    coverage_gaps: list[str]
    plans_needing_update: list[str]
    overall: Literal["aligned", "drift_detected", "block"]
    next_recommended: Literal["commit", "impl", "spec", "none"] | None = None

from pydantic import BaseModel, Field
from typing import Literal


class VisionAuditBrief(BaseModel):
    artifact_path: str = Field(..., description="Path to the spec/plan/diff to audit")
    slug: str = Field(..., description="Short identifier for the work item")
    vision_path: str = Field(default="VISION.md", description="Path to VISION.md")
    work_dir: str = Field(default=".claude/work", description="Output directory for audit report")


class DriftDetail(BaseModel):
    principle_id: Literal["C1", "C2", "C3", "design", "llm", "semantic"]
    description: str
    severity: Literal["block", "warn", "info"]
    suggested_fix: str = ""


class VisionAuditResult(BaseModel):
    verdict: Literal["aligned", "drift_alert", "vision_update_needed"]
    score: int = Field(..., ge=0, le=100)
    slug: str
    drift_details: list[DriftDetail] = []
    patch_suggested: str = ""
    report_path: str = ""

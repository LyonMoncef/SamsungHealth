from typing import Literal

from pydantic import BaseModel, Field


NextRecommended = Literal[
    "spec",
    "tdd",
    "test",
    "impl",
    "review",
    "document",
    "commit",
    "pr",
    "merge",
    "checkpoint",
    "fix",
    "human",
    "release",
    "none",
]


class AgentInputBase(BaseModel):
    version: Literal["1.0"] = "1.0"
    task_id: str = Field(..., description="uuid ou timestamp-slug, sert de clé dir work/")
    invoked_by: str = Field(..., description="skill/agent/human qui déclenche")
    work_dir: str = Field(..., description="work/<task-id>/ path relatif repo")


class AgentOutputBase(BaseModel):
    version: Literal["1.0"] = "1.0"
    task_id: str
    agent: str = Field(..., description="nom de l'agent qui a produit")
    status: Literal["success", "partial", "failed", "needs_clarification"]
    summary: str = Field(..., max_length=500, description="<=500 chars, humain-lisible")
    artifacts: list[str] = Field(default_factory=list, description="file paths produits")
    tokens_used: int = 0
    duration_ms: int = 0
    next_recommended: NextRecommended | None = None
    blockers: list[str] = Field(default_factory=list, description="ce qui a bloqué si status != success")

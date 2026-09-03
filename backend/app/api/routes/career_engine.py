from fastapi import APIRouter
from pydantic import BaseModel, Field

from app.career_engine import SkillAssessment, gap_score, learning_route, priority_score

router = APIRouter(prefix="/career-engine", tags=["career-engine"])


class AssessmentRequest(BaseModel):
    knowledge: float = Field(ge=0, le=100)
    application: float = Field(ge=0, le=100)
    scenario: float = Field(ge=0, le=100)
    evidence: float = Field(ge=0, le=100)


class GapRequest(BaseModel):
    current_score: float = Field(ge=0, le=100)
    required_score: float = Field(ge=0, le=100)
    career_impact: float = Field(ge=0, le=100)
    developability: float = Field(ge=0, le=100)


@router.post("/assess")
def assess(payload: AssessmentRequest):
    assessment = SkillAssessment(**payload.model_dump())
    score = assessment.score()
    return {"score": score, "learning_route": learning_route(score)}


@router.post("/gap")
def gap(payload: GapRequest):
    gap = gap_score(payload.current_score, payload.required_score)
    priority = priority_score(**payload.model_dump())
    return {"gap": gap, "priority": priority, "learning_route": learning_route(payload.current_score)}

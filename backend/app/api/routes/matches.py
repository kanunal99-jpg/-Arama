from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field

from app.auth import get_current_user_id
from app.services.job_matcher import MatchInput, build_reasons, calculate_match_score, match_label

router = APIRouter(prefix="/matches", tags=["matches"])


class MatchRequest(BaseModel):
    skill_fit: float = Field(ge=0, le=100)
    experience_fit: float = Field(ge=0, le=100)
    location_fit: float = Field(ge=0, le=100)
    salary_fit: float = Field(ge=0, le=100)
    career_goal_fit: float = Field(ge=0, le=100)
    missing_skills: list[str] = Field(default_factory=list)


@router.post("/score")
def score_match(payload: MatchRequest, user_id=Depends(get_current_user_id)):
    data = MatchInput(
        skill_fit=payload.skill_fit,
        experience_fit=payload.experience_fit,
        location_fit=payload.location_fit,
        salary_fit=payload.salary_fit,
        career_goal_fit=payload.career_goal_fit,
    )
    score = calculate_match_score(data)
    return {
        "score": score,
        "label": match_label(score),
        "recommendation": "başvur" if score >= 70 else "geliştir ve tekrar değerlendir",
        "reasons": build_reasons(data, payload.missing_skills),
        "weights": {
            "skill_fit": 40,
            "experience_fit": 25,
            "location_fit": 10,
            "salary_fit": 10,
            "career_goal_fit": 15,
        },
    }


@router.get("")
def list_matches(user_id=Depends(get_current_user_id)):
    return {"items": [], "status": "ready"}


@router.post("/run")
def run_matching(user_id=Depends(get_current_user_id)):
    return {"status": "ready", "message": "Matching engine hazır; ilan/profil verisi ile skorlanabilir."}

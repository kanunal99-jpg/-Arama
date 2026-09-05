from typing import Any
from uuid import UUID

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.core.config import settings

router = APIRouter(prefix="/ai", tags=["ai"])

class CoverLetterRequest(BaseModel):
    job_id: UUID
    cv_id: UUID
    tone: str = Field(default="professional", max_length=50)

class CareerPathRequest(BaseModel):
    target_role: str = Field(min_length=2, max_length=255)

class CareerAnalysisRequest(BaseModel):
    target_role: str = Field(min_length=2, max_length=255)
    profile: dict[str, Any]

class InterviewAnswer(BaseModel):
    answer: str = Field(min_length=1, max_length=10000)

@router.post("/cover-letter")
def cover_letter(payload: CoverLetterRequest):
    return {"status": "ai-generation-pending", "job_id": str(payload.job_id), "cv_id": str(payload.cv_id)}

@router.post("/profile-analysis")
def profile_analysis():
    return {"status": "ai-analysis-pending"}

@router.post("/career-path")
def career_path(payload: CareerPathRequest):
    return {"target_role": payload.target_role, "status": "career-engine-pending"}

@router.get("/health")
def ai_health():
    from app.services.ai_career_analysis import verify_openai_connection
    if not settings.openai_api_key:
        raise HTTPException(status_code=503, detail="OPENAI_API_KEY eksik.")
    try:
        return verify_openai_connection()
    except Exception as exc:
        raise HTTPException(status_code=502, detail="OpenAI bağlantı testi başarısız.") from exc

@router.post("/career-analysis")
def career_analysis(payload: CareerAnalysisRequest):
    if not settings.openai_api_key:
        raise HTTPException(status_code=503, detail="OPENAI_API_KEY eksik.")
    from app.services.ai_career_analysis import generate_career_analysis
    try:
        return {"status": "analyzed", "analysis": generate_career_analysis(payload.target_role, payload.profile)}
    except Exception as exc:
        raise HTTPException(status_code=502, detail="AI kariyer analizi üretilemedi.") from exc

@router.post("/interview/start")
def start_interview(job_id: UUID, cv_id: UUID):
    return {"session_id": "pending-session", "job_id": str(job_id), "cv_id": str(cv_id)}

@router.post("/interview/{session_id}/answer")
def answer_interview(session_id: UUID, payload: InterviewAnswer):
    return {"session_id": str(session_id), "status": "evaluation-pending"}

@router.post("/contract/analyze")
def analyze_contract():
    return {"status": "contract-analysis-pending", "disclaimer": "Informational analysis only; not legal advice."}

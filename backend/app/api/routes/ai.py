from fastapi import APIRouter
from pydantic import BaseModel, Field
from uuid import UUID

router = APIRouter(prefix="/ai", tags=["ai"])

class CoverLetterRequest(BaseModel):
    job_id: UUID
    cv_id: UUID
    tone: str = Field(default="professional", max_length=50)

class CareerPathRequest(BaseModel):
    target_role: str = Field(min_length=2, max_length=255)

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

@router.post("/interview/start")
def start_interview(job_id: UUID, cv_id: UUID):
    return {"session_id": "pending-session", "job_id": str(job_id), "cv_id": str(cv_id)}

@router.post("/interview/{session_id}/answer")
def answer_interview(session_id: UUID, payload: InterviewAnswer):
    return {"session_id": str(session_id), "status": "evaluation-pending"}

@router.post("/contract/analyze")
def analyze_contract():
    return {"status": "contract-analysis-pending", "disclaimer": "Informational analysis only; not legal advice."}

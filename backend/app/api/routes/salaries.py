from fastapi import APIRouter
from pydantic import BaseModel, Field

router = APIRouter(prefix="/salary", tags=["salary"])

class SalaryReport(BaseModel):
    role: str = Field(min_length=2, max_length=255)
    country: str = Field(min_length=2, max_length=100)
    city: str | None = Field(default=None, max_length=100)
    experience_years: float = Field(ge=0, le=60)
    salary: float = Field(gt=0)
    currency: str = Field(default="TRY", min_length=3, max_length=10)

@router.get("/estimate")
def salary_estimate(role: str, country: str, experience_years: float = 0):
    return {"role": role, "country": country, "experience_years": experience_years, "status": "estimation-engine-pending"}

@router.post("/report", status_code=201)
def submit_salary_report(payload: SalaryReport):
    return {"accepted": True, "anonymous": True, "report": payload.model_dump()}

@router.get("/market")
def salary_market(role: str, country: str):
    return {"role": role, "country": country, "status": "market-data-pending"}

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from uuid import UUID

router = APIRouter(prefix="/applications", tags=["applications"])

class ApplicationCreate(BaseModel):
    job_id: UUID
    status: str = Field(default="saved", max_length=50)
    notes: str | None = None

class ApplicationUpdate(BaseModel):
    status: str | None = Field(default=None, max_length=50)
    notes: str | None = None

@router.get("")
def list_applications():
    return {"items": [], "total": 0}

@router.post("", status_code=201)
def create_application(payload: ApplicationCreate):
    return {"id": "pending-persistence", **payload.model_dump()}

@router.patch("/{application_id}")
def update_application(application_id: UUID, payload: ApplicationUpdate):
    return {"id": str(application_id), **payload.model_dump(exclude_none=True)}

@router.get("/{application_id}")
def get_application(application_id: UUID):
    raise HTTPException(status_code=404, detail="Application not found")

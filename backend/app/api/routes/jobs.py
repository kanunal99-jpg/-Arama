from fastapi import APIRouter
router = APIRouter(prefix="/jobs", tags=["jobs"])

@router.get("")
def list_jobs(limit: int = 20):
    return {"items": [], "limit": limit}

@router.get("/recommended")
def recommended_jobs():
    return {"items": []}

@router.get("/{job_id}")
def get_job(job_id: str):
    return {"id": job_id}

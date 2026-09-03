from fastapi import APIRouter
router = APIRouter(prefix="/matches", tags=["matches"])

@router.get("")
def list_matches():
    return {"items": []}

@router.post("/run")
def run_matching():
    return {"status": "queued"}

from fastapi import APIRouter
router = APIRouter(prefix="/users", tags=["users"])

@router.get("/me")
def me():
    return {"id": None, "message": "Authentication layer pending"}

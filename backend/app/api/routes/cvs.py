from fastapi import APIRouter, UploadFile, File
router = APIRouter(prefix="/cvs", tags=["cvs"])

@router.post("")
async def upload_cv(file: UploadFile = File(...)):
    return {"filename": file.filename, "status": "accepted", "next": "parse"}

@router.post("/{cv_id}/parse")
def parse_cv(cv_id: str):
    return {"cv_id": cv_id, "status": "queued"}

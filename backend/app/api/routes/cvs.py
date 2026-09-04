from fastapi import APIRouter, File, HTTPException, UploadFile

from app.services.cv_parser import MAX_CV_BYTES, parse_cv

router = APIRouter(prefix="/cvs", tags=["cvs"])


@router.post("/analyze")
async def analyze_cv(file: UploadFile = File(...)):
    """Extract a structured career profile from a PDF, DOCX or TXT CV."""
    if not file.filename:
        raise HTTPException(status_code=400, detail="CV dosya adı bulunamadı.")

    data = await file.read(MAX_CV_BYTES + 1)
    if len(data) > MAX_CV_BYTES:
        raise HTTPException(status_code=413, detail="CV dosyası 10 MB sınırını aşıyor.")

    try:
        result = parse_cv(file.filename, data)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=422, detail="CV okunamadı veya dosya bozuk.") from exc

    return {"status": "analyzed", **result}


@router.post("")
async def upload_cv(file: UploadFile = File(...)):
    """Backward-compatible upload endpoint; parsing is now available through /analyze."""
    return {"filename": file.filename, "status": "accepted", "next": "analyze"}


@router.post("/{cv_id}/parse")
def parse_cv_legacy(cv_id: str):
    return {"cv_id": cv_id, "status": "queued"}

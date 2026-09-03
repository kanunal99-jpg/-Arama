from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from uuid import UUID

security = HTTPBearer(auto_error=False)


def get_current_user_id(credentials: HTTPAuthorizationCredentials = Depends(security)) -> UUID:
    if not credentials:
        raise HTTPException(status_code=401, detail="Authentication required")
    try:
        return UUID(credentials.credentials)
    except ValueError as exc:
        raise HTTPException(status_code=401, detail="Invalid bearer token") from exc

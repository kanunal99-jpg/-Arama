from fastapi import APIRouter
from pydantic import BaseModel, Field

router = APIRouter(prefix="/github", tags=["github"])

class GitHubProfile(BaseModel):
    username: str = Field(min_length=1, max_length=100)

@router.post("/analyze")
def analyze_github(payload: GitHubProfile):
    return {"username": payload.username, "status": "github-analysis-pending"}

@router.get("/{username}")
def get_github_profile(username: str):
    return {"username": username, "status": "github-integration-pending"}

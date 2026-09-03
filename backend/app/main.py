from fastapi import FastAPI
from app.api.routes import health, users, cvs, jobs, matches

app = FastAPI(title="Arama API", version="0.1.0")
app.include_router(health.router, prefix="/api/v1")
app.include_router(users.router, prefix="/api/v1")
app.include_router(cvs.router, prefix="/api/v1")
app.include_router(jobs.router, prefix="/api/v1")
app.include_router(matches.router, prefix="/api/v1")

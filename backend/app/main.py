from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.routes import health, users, cvs, jobs, matches, applications, salaries, ai, career_engine

app = FastAPI(title="Arama API", version="0.3.0", description="AI Career Intelligence Platform API")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["*"],
)

for router in (health.router, users.router, cvs.router, jobs.router, matches.router, applications.router, salaries.router, ai.router, career_engine.router):
    app.include_router(router, prefix="/api/v1")


@app.get("/")
def root():
    return {"name": "Arama API", "version": "0.3.0", "status": "ok"}

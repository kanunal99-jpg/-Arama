from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.auth import get_current_user_id
from app.career_engine import SkillAssessment, gap_score, learning_route, priority_score
from app.db import get_db

router = APIRouter(prefix="/career-engine", tags=["career-engine"])

class AssessmentRequest(BaseModel):
    skill_id: UUID
    knowledge: float = Field(ge=0, le=100)
    application: float = Field(ge=0, le=100)
    scenario: float = Field(ge=0, le=100)
    evidence: float = Field(ge=0, le=100)

class GoalSkillRequest(BaseModel):
    career_goal_id: UUID
    skill_id: UUID
    required_score: float = Field(ge=0, le=100)
    career_impact: float = Field(ge=0, le=100)
    developability: float = Field(ge=0, le=100)

@router.post("/assess")
def assess(payload: AssessmentRequest, user_id: UUID = Depends(get_current_user_id), db: Session = Depends(get_db)):
    if not db.execute(text("SELECT 1 FROM users WHERE id=:id"), {"id": str(user_id)}).scalar():
        raise HTTPException(401, "User not found")
    if not db.execute(text("SELECT 1 FROM skills WHERE id=:id"), {"id": str(payload.skill_id)}).scalar():
        raise HTTPException(404, "Skill not found")
    score = SkillAssessment(payload.knowledge, payload.application, payload.scenario, payload.evidence).score()
    db.execute(text("""INSERT INTO skill_assessments(user_id,skill_id,knowledge,application,scenario,evidence,score)
                      VALUES(:u,:s,:k,:a,:sc,:e,:score)"""),
               {"u": str(user_id), "s": str(payload.skill_id), "k": payload.knowledge, "a": payload.application,
                "sc": payload.scenario, "e": payload.evidence, "score": score})
    db.execute(text("INSERT INTO skill_score_history(user_id,skill_id,score,source) VALUES(:u,:s,:score,'assessment')"),
               {"u": str(user_id), "s": str(payload.skill_id), "score": score})
    db.commit()
    return {"score": score, "learning_route": learning_route(score), "persisted": True}

@router.post("/goal-skill")
def goal_skill(payload: GoalSkillRequest, user_id: UUID = Depends(get_current_user_id), db: Session = Depends(get_db)):
    owned = db.execute(text("SELECT 1 FROM career_goals WHERE id=:g AND user_id=:u"), {"g": str(payload.career_goal_id), "u": str(user_id)}).scalar()
    if not owned:
        raise HTTPException(404, "Career goal not found")
    db.execute(text("""INSERT INTO career_goal_skills(career_goal_id,skill_id,required_score,career_impact,developability)
                      VALUES(:g,:s,:r,:i,:d)
                      ON CONFLICT(career_goal_id,skill_id) DO UPDATE SET required_score=EXCLUDED.required_score,
                      career_impact=EXCLUDED.career_impact, developability=EXCLUDED.developability"""),
               {"g": str(payload.career_goal_id), "s": str(payload.skill_id), "r": payload.required_score,
                "i": payload.career_impact, "d": payload.developability})
    db.commit()
    return {"saved": True}

@router.get("/dashboard")
def dashboard(user_id: UUID = Depends(get_current_user_id), db: Session = Depends(get_db)):
    rows = db.execute(text("""SELECT s.id skill_id,s.name,cgs.required_score,cgs.career_impact,cgs.developability,
               COALESCE(a.score,0) current_score FROM career_goals cg
               JOIN career_goal_skills cgs ON cgs.career_goal_id=cg.id JOIN skills s ON s.id=cgs.skill_id
               LEFT JOIN LATERAL (SELECT score FROM skill_assessments x WHERE x.user_id=cg.user_id AND x.skill_id=cgs.skill_id ORDER BY created_at DESC LIMIT 1) a ON TRUE
               WHERE cg.user_id=:u AND cg.is_active=TRUE"""), {"u": str(user_id)}).mappings().all()
    items=[]
    for r in rows:
        cur=float(r["current_score"]); req=float(r["required_score"])
        items.append({"skill_id":str(r["skill_id"]),"skill":r["name"],"current_score":cur,"required_score":req,
                      "gap":gap_score(cur,req),"priority":priority_score(cur,req,float(r["career_impact"]),float(r["developability"])),
                      "learning_route":learning_route(cur)})
    items.sort(key=lambda x:x["priority"], reverse=True)
    return {"items":items,"next_priority":items[0] if items else None}

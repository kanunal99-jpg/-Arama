ALTER TABLE career_goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE skill_assessments ENABLE ROW LEVEL SECURITY;
ALTER TABLE skill_score_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE skill_evidence ENABLE ROW LEVEL SECURITY;

-- The FastAPI service connects as a trusted server role and validates user ownership.
-- Policies are intentionally not added here because the existing users table is the
-- application's identity table rather than auth.users.

CREATE INDEX IF NOT EXISTS idx_career_goals_user_active ON career_goals(user_id, is_active);

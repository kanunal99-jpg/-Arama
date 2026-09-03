CREATE TABLE IF NOT EXISTS career_goals (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 target_role VARCHAR(255) NOT NULL,
 target_score NUMERIC(5,2) NOT NULL DEFAULT 80 CHECK (target_score BETWEEN 0 AND 100),
 is_active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS skill_assessments (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
 knowledge NUMERIC(5,2) NOT NULL CHECK (knowledge BETWEEN 0 AND 100),
 application NUMERIC(5,2) NOT NULL CHECK (application BETWEEN 0 AND 100),
 scenario NUMERIC(5,2) NOT NULL CHECK (scenario BETWEEN 0 AND 100),
 evidence NUMERIC(5,2) NOT NULL CHECK (evidence BETWEEN 0 AND 100),
 score NUMERIC(5,2) NOT NULL CHECK (score BETWEEN 0 AND 100),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS career_goal_skills (
 career_goal_id UUID NOT NULL REFERENCES career_goals(id) ON DELETE CASCADE,
 skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
 required_score NUMERIC(5,2) NOT NULL CHECK (required_score BETWEEN 0 AND 100),
 career_impact NUMERIC(5,2) NOT NULL DEFAULT 50 CHECK (career_impact BETWEEN 0 AND 100),
 developability NUMERIC(5,2) NOT NULL DEFAULT 50 CHECK (developability BETWEEN 0 AND 100),
 PRIMARY KEY (career_goal_id, skill_id)
);

CREATE TABLE IF NOT EXISTS skill_score_history (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
 score NUMERIC(5,2) NOT NULL CHECK (score BETWEEN 0 AND 100),
 source VARCHAR(50) NOT NULL DEFAULT 'assessment',
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS learning_modules (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
 title VARCHAR(255) NOT NULL,
 difficulty VARCHAR(30) NOT NULL DEFAULT 'adaptive',
 content JSONB NOT NULL DEFAULT '{}'::jsonb,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS skill_evidence (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
 evidence_type VARCHAR(50) NOT NULL,
 title VARCHAR(255) NOT NULL,
 url TEXT,
 description TEXT,
 verified BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_assessments_user_skill ON skill_assessments(user_id, skill_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_score_history_user_skill ON skill_score_history(user_id, skill_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_goal_skills_goal ON career_goal_skills(career_goal_id);
CREATE INDEX IF NOT EXISTS idx_evidence_user_skill ON skill_evidence(user_id, skill_id);

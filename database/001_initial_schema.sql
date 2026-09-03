CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 email VARCHAR(255) UNIQUE NOT NULL,
 password_hash TEXT,
 first_name VARCHAR(100),
 last_name VARCHAR(100),
 role VARCHAR(30) NOT NULL DEFAULT 'user',
 country VARCHAR(100), city VARCHAR(100), timezone VARCHAR(100),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cvs (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 file_url TEXT, file_name VARCHAR(255), parsed_text TEXT, summary TEXT,
 experience_years NUMERIC(5,2), education JSONB, parsed_data JSONB,
 embedding_id VARCHAR(255), version INTEGER NOT NULL DEFAULT 1,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS skills (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 name VARCHAR(255) UNIQUE NOT NULL,
 normalized_name VARCHAR(255) UNIQUE NOT NULL,
 category VARCHAR(100), aliases JSONB, description TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS job_listings (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 source VARCHAR(100) NOT NULL,
 external_id VARCHAR(255), company_name VARCHAR(255), title VARCHAR(500) NOT NULL,
 description TEXT, location VARCHAR(255), country VARCHAR(100), remote_type VARCHAR(50),
 employment_type VARCHAR(50), salary_min NUMERIC(12,2), salary_max NUMERIC(12,2),
 salary_currency VARCHAR(10), application_url TEXT, published_at TIMESTAMPTZ,
 expires_at TIMESTAMPTZ, fraud_score NUMERIC(5,2), raw_data JSONB,
 embedding_id VARCHAR(255), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE(source, external_id)
);

CREATE TABLE IF NOT EXISTS matches (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 user_id UUID REFERENCES users(id) ON DELETE CASCADE,
 cv_id UUID REFERENCES cvs(id) ON DELETE SET NULL,
 job_id UUID REFERENCES job_listings(id) ON DELETE CASCADE,
 score NUMERIC(5,2), semantic_score NUMERIC(5,2), skill_score NUMERIC(5,2),
 experience_score NUMERIC(5,2), location_score NUMERIC(5,2), salary_score NUMERIC(5,2),
 explanation TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE(user_id, job_id)
);

CREATE TABLE IF NOT EXISTS applications (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 user_id UUID REFERENCES users(id) ON DELETE CASCADE,
 job_id UUID REFERENCES job_listings(id) ON DELETE CASCADE,
 status VARCHAR(50) NOT NULL DEFAULT 'saved', cover_letter TEXT, notes TEXT,
 applied_at TIMESTAMPTZ, interview_date TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS salary_reports (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 user_id UUID REFERENCES users(id) ON DELETE SET NULL,
 role VARCHAR(255), country VARCHAR(100), city VARCHAR(100),
 experience_years NUMERIC(5,2), salary NUMERIC(12,2), currency VARCHAR(10),
 employment_type VARCHAR(50), anonymous BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_activities (
 id BIGSERIAL PRIMARY KEY,
 user_id UUID REFERENCES users(id) ON DELETE CASCADE,
 activity_type VARCHAR(100), entity_type VARCHAR(100), entity_id UUID,
 metadata JSONB, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cvs_user ON cvs(user_id);
CREATE INDEX IF NOT EXISTS idx_jobs_country ON job_listings(country);
CREATE INDEX IF NOT EXISTS idx_jobs_published ON job_listings(published_at DESC);
CREATE INDEX IF NOT EXISTS idx_matches_user_score ON matches(user_id, score DESC);
CREATE INDEX IF NOT EXISTS idx_applications_user ON applications(user_id);
CREATE INDEX IF NOT EXISTS idx_salary_market ON salary_reports(role, country);

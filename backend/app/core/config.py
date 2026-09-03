from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Arama API"
    app_version: str = "0.4.0"
    environment: str = "development"
    database_url: str = "postgresql+psycopg://arama:change-me@localhost:5432/arama"
    supabase_db_url: str | None = None
    redis_url: str = "redis://localhost:6379/0"
    qdrant_url: str = "http://localhost:6333"
    openai_api_key: str | None = None
    allowed_origins: str = "http://localhost:3000"

    @property
    def effective_database_url(self) -> str:
        return self.supabase_db_url or self.database_url

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


settings = Settings()

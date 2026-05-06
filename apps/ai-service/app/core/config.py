from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_name: str = "ai-service"
    app_version: str = "1.0.0"
    api_v1_prefix: str = "/api/v1"
    model_path: str = "/app/models"
    port: int = 8000
    synthea_csv_dir: str | None = None


settings = Settings()

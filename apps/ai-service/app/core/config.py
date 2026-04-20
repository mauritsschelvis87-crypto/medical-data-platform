from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "ai-service"
    app_version: str = "1.0.0"
    api_v1_prefix: str = "/api/v1"


settings = Settings()
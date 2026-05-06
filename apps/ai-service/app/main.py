from fastapi import FastAPI

from app.api.v1.endpoints.patients import router as patients_router
from app.api.v1.endpoints.health import router as health_router
from app.api.v1.endpoints.predictions import router as prediction_router

from app.core.config import settings
from app.core.logging_config import setup_logging

setup_logging()

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version
)

API_PREFIX = settings.api_v1_prefix

app.include_router(health_router, prefix=API_PREFIX)
app.include_router(prediction_router, prefix=API_PREFIX)
app.include_router(patients_router, prefix=API_PREFIX)

@app.get("/")
def root():
    return {"status": "ok"}
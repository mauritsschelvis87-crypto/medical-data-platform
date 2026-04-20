from fastapi import APIRouter

from app.dto.requests.prediction_request import PredictionRequest
from app.dto.responses.prediction_response import PredictionResponse
from app.services.model_registry_service import ModelRegistryService
from app.services.prediction_service import PredictionService

router = APIRouter()

model_registry = ModelRegistryService()
prediction_service = PredictionService(model_registry)


@router.post("/predictions/calculate", response_model=PredictionResponse)
def calculate_predictions(request: PredictionRequest):
    return prediction_service.calculate_predictions(request)
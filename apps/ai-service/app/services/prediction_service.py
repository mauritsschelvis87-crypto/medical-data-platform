from app.dto.requests.prediction_request import PredictionRequest
from app.dto.responses.prediction_response import PredictionResponse, PredictionItemResponse
from app.services.feature_mapping_service import FeatureMappingService
from app.services.model_registry_service import ModelRegistryService
from app.utils.datetime_utils import now_iso


class PredictionService:

    def __init__(self, registry: ModelRegistryService):
        self.registry = registry

    def calculate_predictions(self, request: PredictionRequest) -> PredictionResponse:
        features = FeatureMappingService.map(request.features)

        results = []

        for i, p_type in enumerate(request.prediction_types):
            predictor = self.registry.get(p_type)

            result = predictor.predict(features, is_main=(i == 0))

            results.append(PredictionItemResponse(
                predictionType=result.prediction_type,
                riskLevel=result.risk_level,
                riskScore=result.risk_score,
                confidence=result.confidence,
                explanation=result.explanation,
                isMainPrediction=result.is_main_prediction
            ))

        return PredictionResponse(
            patientId=request.patient_id,
            generatedAt=now_iso(),
            predictions=results
        )
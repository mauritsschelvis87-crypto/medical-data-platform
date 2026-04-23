from app.domain.enums.prediction_type import PredictionType
from app.domain.models.prediction_result import PredictionResult
from app.services.risk_evaluation_service import RiskEvaluationService


class GeneralDeteriorationPredictor:

    def predict(self, features, is_main):
        score = min(
            1.0,
            (
                max(0.0, features["heart_rate"] - 80) / 80
                + max(0.0, features["temperature"] - 37.0) / 3
                + max(0.0, 95 - features["oxygen_saturation"]) / 10
                + min(features["recent_consult_count"], 5) / 10
            ) / 3
        )

        return PredictionResult(
            prediction_type=PredictionType.GENERAL_DETERIORATION,
            risk_level=RiskEvaluationService.evaluate(score),
            risk_score=score,
            confidence=0.79,
            explanation="Heart rate, temperature, oxygen saturation and recent consult load",
            is_main_prediction=is_main
        )

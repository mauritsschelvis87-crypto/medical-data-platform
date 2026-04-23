from app.domain.enums.prediction_type import PredictionType
from app.domain.models.prediction_result import PredictionResult
from app.services.risk_evaluation_service import RiskEvaluationService


class RespiratoryPredictor:

    def predict(self, features, is_main):
        score = min(
            1.0,
            (
                max(0.0, 96 - features["oxygen_saturation"]) / 8
                + max(0.0, features["heart_rate"] - 85) / 85
            ) / 2
        )

        return PredictionResult(
            prediction_type=PredictionType.RESPIRATORY_RISK,
            risk_level=RiskEvaluationService.evaluate(score),
            risk_score=score,
            confidence=0.78,
            explanation="Respiratory heuristic based on oxygen saturation and heart rate",
            is_main_prediction=is_main
        )

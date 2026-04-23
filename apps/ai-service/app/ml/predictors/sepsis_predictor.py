from app.domain.enums.prediction_type import PredictionType
from app.domain.models.prediction_result import PredictionResult
from app.services.risk_evaluation_service import RiskEvaluationService


class SepsisPredictor:

    def predict(self, features, is_main):
        score = min(
            1.0,
            (
                max(0.0, features["temperature"] - 37.5) / 2.5
                + max(0.0, features["heart_rate"] - 90) / 70
            ) / 2
        )

        return PredictionResult(
            prediction_type=PredictionType.SEPSIS_RISK,
            risk_level=RiskEvaluationService.evaluate(score),
            risk_score=score,
            confidence=0.74,
            explanation="Sepsis heuristic based on temperature and heart rate",
            is_main_prediction=is_main
        )

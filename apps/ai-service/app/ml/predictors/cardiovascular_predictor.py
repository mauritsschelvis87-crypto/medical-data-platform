from app.domain.enums.prediction_type import PredictionType
from app.domain.models.prediction_result import PredictionResult
from app.services.risk_evaluation_service import RiskEvaluationService


class CardiovascularPredictor:

    def predict(self, features, is_main):
        score = min(1.0, (features["systolic"] + features["cholesterol"]) / 300)

        return PredictionResult(
            prediction_type=PredictionType.CARDIOVASCULAR,
            risk_level=RiskEvaluationService.evaluate(score),
            risk_score=score,
            confidence=0.82,
            explanation="Blood pressure + cholesterol",
            is_main_prediction=is_main
        )
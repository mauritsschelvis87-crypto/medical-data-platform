from app.domain.enums.prediction_type import PredictionType
from app.domain.models.prediction_result import PredictionResult
from app.services.risk_evaluation_service import RiskEvaluationService


class BmiRiskPredictor:

    def predict(self, features, is_main):
        score = min(1.0, features["bmi"] / 40)

        return PredictionResult(
            prediction_type=PredictionType.BMI_RISK,
            risk_level=RiskEvaluationService.evaluate(score),
            risk_score=score,
            confidence=0.80,
            explanation="BMI based risk",
            is_main_prediction=is_main
        )
from app.domain.enums.prediction_type import PredictionType
from app.domain.models.prediction_result import PredictionResult
from app.services.risk_evaluation_service import RiskEvaluationService


class DiabetesPredictor:

    def predict(self, features, is_main):
        score = min(1.0, (features["glucose"] + features["bmi"]) / 200)

        return PredictionResult(
            prediction_type=PredictionType.DIABETES,
            risk_level=RiskEvaluationService.evaluate(score),
            risk_score=score,
            confidence=0.85,
            explanation="Glucose + BMI based estimation",
            is_main_prediction=is_main
        )
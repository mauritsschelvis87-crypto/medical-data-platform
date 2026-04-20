from app.domain.enums.prediction_type import PredictionType
from app.ml.predictors.diabetes_predictor import DiabetesPredictor
from app.ml.predictors.cardiovascular_predictor import CardiovascularPredictor
from app.ml.predictors.bmi_risk_predictor import BmiRiskPredictor


class ModelRegistryService:

    def __init__(self):
        self.registry = {
            PredictionType.DIABETES: DiabetesPredictor(),
            PredictionType.CARDIOVASCULAR: CardiovascularPredictor(),
            PredictionType.BMI_RISK: BmiRiskPredictor()
        }

    def get(self, prediction_type: PredictionType):
        return self.registry[prediction_type]
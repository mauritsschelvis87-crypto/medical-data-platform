from app.domain.enums.prediction_type import PredictionType
from app.ml.predictors.diabetes_predictor import DiabetesPredictor
from app.ml.predictors.cardiovascular_predictor import CardiovascularPredictor
from app.ml.predictors.general_deterioration_predictor import GeneralDeteriorationPredictor
from app.ml.predictors.respiratory_predictor import RespiratoryPredictor
from app.ml.predictors.sepsis_predictor import SepsisPredictor


class ModelRegistryService:

    def __init__(self):
        self.registry = {
            PredictionType.DIABETES_RISK: DiabetesPredictor(),
            PredictionType.CARDIOVASCULAR_RISK: CardiovascularPredictor(),
            PredictionType.GENERAL_DETERIORATION: GeneralDeteriorationPredictor(),
            PredictionType.SEPSIS_RISK: SepsisPredictor(),
            PredictionType.RESPIRATORY_RISK: RespiratoryPredictor(),
        }

    def get(self, prediction_type: PredictionType):
        return self.registry[prediction_type]

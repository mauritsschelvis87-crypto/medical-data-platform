from pydantic import BaseModel

from app.domain.enums.prediction_type import PredictionType
from app.domain.enums.risk_level import RiskLevel


class PredictionResult(BaseModel):
    prediction_type: PredictionType
    risk_level: RiskLevel
    risk_score: float
    confidence: float
    explanation: str
    is_main_prediction: bool
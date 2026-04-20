from pydantic import BaseModel, Field
from typing import List

from app.domain.enums.prediction_type import PredictionType
from app.domain.enums.risk_level import RiskLevel


class PredictionItemResponse(BaseModel):
    prediction_type: PredictionType = Field(alias="predictionType")
    risk_level: RiskLevel = Field(alias="riskLevel")
    risk_score: float = Field(alias="riskScore")
    confidence: float
    explanation: str
    is_main_prediction: bool = Field(alias="isMainPrediction")

    model_config = {"populate_by_name": True}


class PredictionResponse(BaseModel):
    patient_id: int = Field(alias="patientId")
    generated_at: str = Field(alias="generatedAt")
    predictions: List[PredictionItemResponse]

    model_config = {"populate_by_name": True}
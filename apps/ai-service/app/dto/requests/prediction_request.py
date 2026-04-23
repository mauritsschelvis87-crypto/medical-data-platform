from pydantic import BaseModel, Field
from typing import Any, Dict, List

from app.domain.enums.prediction_type import PredictionType


class PredictionRequest(BaseModel):
    patient_id: str = Field(alias="patientId")
    trigger_source: str = Field(alias="triggerSource")
    prediction_types: List[PredictionType] = Field(alias="predictionTypes")
    features: Dict[str, Any]

    model_config = {"populate_by_name": True}

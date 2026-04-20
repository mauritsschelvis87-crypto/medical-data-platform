from pydantic import BaseModel, Field
from typing import List, Optional

from app.domain.enums.prediction_type import PredictionType


class FeatureInput(BaseModel):
    age: Optional[int] = None
    gender: Optional[str] = None
    bmi: Optional[float] = None
    weight: Optional[float] = None
    heart_rate: Optional[int] = Field(default=None, alias="heartRate")
    temperature: Optional[float] = None
    glucose: Optional[float] = None
    systolic_blood_pressure: Optional[int] = Field(default=None, alias="systolicBloodPressure")
    diastolic_blood_pressure: Optional[int] = Field(default=None, alias="diastolicBloodPressure")
    oxygen_saturation: Optional[int] = Field(default=None, alias="oxygenSaturation")
    cholesterol: Optional[float] = None

    model_config = {"populate_by_name": True}


class PredictionRequest(BaseModel):
    patient_id: int = Field(alias="patientId")
    trigger_source: str = Field(alias="triggerSource")
    prediction_types: List[PredictionType] = Field(alias="predictionTypes")
    features: FeatureInput

    model_config = {"populate_by_name": True}
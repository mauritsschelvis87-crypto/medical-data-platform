from enum import Enum


class PredictionType(str, Enum):
    DIABETES = "DIABETES"
    CARDIOVASCULAR = "CARDIOVASCULAR"
    BMI_RISK = "BMI_RISK"
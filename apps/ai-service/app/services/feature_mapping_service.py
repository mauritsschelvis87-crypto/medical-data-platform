from app.dto.requests.prediction_request import FeatureInput


class FeatureMappingService:

    @staticmethod
    def map(features: FeatureInput):
        return {
            "age": features.age or 0,
            "bmi": features.bmi or 0,
            "glucose": features.glucose or 0,
            "systolic": features.systolic_blood_pressure or 0,
            "diastolic": features.diastolic_blood_pressure or 0,
            "cholesterol": features.cholesterol or 0,
        }
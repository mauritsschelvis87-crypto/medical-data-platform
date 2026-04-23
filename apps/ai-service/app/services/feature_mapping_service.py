class FeatureMappingService:

    @staticmethod
    def map(features: dict):
        return {
            "age": FeatureMappingService._number(features, "age"),
            "bmi": FeatureMappingService._number(features, "bmi"),
            "weight": FeatureMappingService._number(features, "weight"),
            "glucose": FeatureMappingService._number(features, "glucose"),
            "temperature": FeatureMappingService._number(features, "temperature"),
            "heart_rate": FeatureMappingService._number(features, "heartRate", "heart_rate"),
            "systolic": FeatureMappingService._number(features, "systolicBloodPressure", "bloodPressureSystolic", "systolic_blood_pressure"),
            "diastolic": FeatureMappingService._number(features, "diastolicBloodPressure", "bloodPressureDiastolic", "diastolic_blood_pressure"),
            "oxygen_saturation": FeatureMappingService._number(features, "oxygenSaturation", "oxygen_saturation"),
            "cholesterol": FeatureMappingService._number(features, "cholesterol"),
            "recent_consult_count": FeatureMappingService._number(features, "recentConsultCount", "recent_consult_count"),
        }

    @staticmethod
    def _number(features: dict, *keys: str) -> float:
        for key in keys:
            value = features.get(key)
            if value is None or value == "":
                continue
            try:
                return float(value)
            except (TypeError, ValueError):
                continue
        return 0.0

from app.domain.enums.risk_level import RiskLevel


class RiskEvaluationService:

    @staticmethod
    def evaluate(score: float) -> RiskLevel:
        if score > 0.8:
            return RiskLevel.CRITICAL
        if score > 0.6:
            return RiskLevel.HIGH
        if score > 0.4:
            return RiskLevel.MEDIUM
        return RiskLevel.LOW
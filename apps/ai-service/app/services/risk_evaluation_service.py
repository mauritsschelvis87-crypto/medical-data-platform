from app.domain.enums.risk_level import RiskLevel


class RiskEvaluationService:

    @staticmethod
    def evaluate(score: float) -> RiskLevel:
        if score is None:
            return RiskLevel.NEUTRAL

        if score >= 0.30:
            return RiskLevel.HIGH

        if score >= 0.15:
            return RiskLevel.MEDIUM

        if score >= 0.05:
            return RiskLevel.LOW

        return RiskLevel.NEUTRAL
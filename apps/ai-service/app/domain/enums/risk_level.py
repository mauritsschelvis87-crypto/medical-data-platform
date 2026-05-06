from enum import Enum


class RiskLevel(str, Enum):
    NEUTRAL = "NEUTRAL"
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
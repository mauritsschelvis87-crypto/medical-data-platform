package com.mauri.backend.mapper;

import com.mauri.backend.dto.prediction.PredictionDto;
import com.mauri.backend.entity.Prediction;
import com.mauri.backend.enums.PredictionType;
import com.mauri.backend.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class PredictionMapper {

    public PredictionDto toDto(Prediction prediction) {
        if (prediction == null) {
            return null;
        }

        PredictionDto dto = new PredictionDto();
        dto.setId(prediction.getId());
        dto.setPredictionType(prediction.getPredictionType() != null ? prediction.getPredictionType().name() : null);
        
        // Effective risk level (mapping internal CRITICAL to HIGH)
        RiskLevel rawLevel = prediction.getRiskLevel();
        RiskLevel displayLevel = mapToDisplayRiskLevel(rawLevel);
        
        dto.setRiskLevel(displayLevel.name());
        dto.setRiskScore(prediction.getRiskScore());
        dto.setConfidence(prediction.getConfidence());
        dto.setExplanation(prediction.getExplanation());
        dto.setMainPrediction(prediction.isMainPrediction());
        dto.setPredictionTimestamp(prediction.getPredictionTimestamp());

        // Risk comparison + model metadata
        dto.setPreviousRiskLevel(prediction.getPreviousRiskLevel() != null ? prediction.getPreviousRiskLevel().name() : null);
        dto.setRiskIncreased(prediction.isRiskIncreased());
        dto.setModelVersion(prediction.getModelVersion());
        
        // Visual representation fields (Exact 4 states)
        dto.setVisibleInSummary(displayLevel != RiskLevel.NEUTRAL);
        dto.setDisplayRiskText(getDisplayRiskText(displayLevel));
        dto.setStatusColor(getStatusColor(displayLevel));

        return dto;
    }

    private RiskLevel mapToDisplayRiskLevel(RiskLevel level) {
        if (level == null) return RiskLevel.NEUTRAL;
        if (level == RiskLevel.CRITICAL) return RiskLevel.HIGH;
        return level;
    }

    private String getDisplayRiskText(RiskLevel level) {
        return switch (level) {
            case LOW -> "LOW";
            case MEDIUM -> "MEDIUM";
            case HIGH -> "HIGH";
            default -> null; // NEUTRAL does not show text
        };
    }

    private String getStatusColor(RiskLevel level) {
        return switch (level) {
            case NEUTRAL -> "neutral";
            case LOW -> "subtle";
            case MEDIUM -> "warning";
            case HIGH -> "danger";
            default -> "neutral";
        };
    }

    public Prediction toEntity(PredictionDto dto) {
        if (dto == null) {
            return null;
        }

        Prediction entity = new Prediction();
        entity.setId(dto.getId());
        entity.setPredictionType(toPredictionType(dto.getPredictionType()));
        entity.setRiskLevel(toRiskLevel(dto.getRiskLevel()));

        entity.setRiskScore(dto.getRiskScore());
        entity.setConfidence(dto.getConfidence());
        entity.setExplanation(dto.getExplanation());
        entity.setMainPrediction(dto.isMainPrediction());
        entity.setPredictionTimestamp(dto.getPredictionTimestamp());

        entity.setPreviousRiskLevel(toRiskLevel(dto.getPreviousRiskLevel()));
        entity.setRiskIncreased(dto.isRiskIncreased());
        entity.setModelVersion(dto.getModelVersion());

        return entity;
    }

    private PredictionType toPredictionType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PredictionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported prediction type: " + value, exception);
        }
    }

    private RiskLevel toRiskLevel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return RiskLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported risk level: " + value, exception);
        }
    }
}

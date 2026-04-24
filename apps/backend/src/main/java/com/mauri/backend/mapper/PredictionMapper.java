package com.mauri.backend.mapper;

import com.mauri.backend.dto.prediction.PredictionDto;
import com.mauri.backend.entity.Prediction;
import com.mauri.backend.enums.PredictionType;
import com.mauri.backend.enums.RiskLevel;
import org.springframework.stereotype.Component;

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
        dto.setRiskLevel(prediction.getRiskLevel() != null ? prediction.getRiskLevel().name() : null);
        dto.setRiskScore(prediction.getRiskScore());
        dto.setConfidence(prediction.getConfidence());
        dto.setExplanation(prediction.getExplanation());
        dto.setMainPrediction(prediction.isMainPrediction());
        dto.setPredictionTimestamp(prediction.getPredictionTimestamp());

        // Risk comparison + warning system
        dto.setPreviousRiskLevel(prediction.getPreviousRiskLevel() != null ? prediction.getPreviousRiskLevel().name() : null);
        dto.setRiskIncreased(prediction.isRiskIncreased());
        dto.setRequiresConfirmation(prediction.isRequiresConfirmation());
        dto.setConfirmed(prediction.isConfirmed());
        dto.setConfirmedAt(prediction.getConfirmedAt());
        dto.setConfirmedBy(prediction.getConfirmedBy());
        dto.setModelVersion(prediction.getModelVersion());
        dto.setThresholdTriggered(prediction.isThresholdTriggered());

        return dto;
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
        entity.setRequiresConfirmation(dto.isRequiresConfirmation());
        entity.setConfirmed(dto.isConfirmed());
        entity.setConfirmedAt(dto.getConfirmedAt());
        entity.setConfirmedBy(dto.getConfirmedBy());
        entity.setModelVersion(dto.getModelVersion());
        entity.setThresholdTriggered(dto.isThresholdTriggered());

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

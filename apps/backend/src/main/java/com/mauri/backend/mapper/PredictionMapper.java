package com.mauri.backend.mapper;

import com.mauri.backend.dto.prediction.PredictionDto;
import com.mauri.backend.entity.Prediction;
import com.mauri.backend.enums.PredictionType;
import com.mauri.backend.enums.RiskLevel;
import org.springframework.stereotype.Component;

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
        
        if (dto.getPredictionType() != null) {
            entity.setPredictionType(PredictionType.valueOf(dto.getPredictionType()));
        }
        
        if (dto.getRiskLevel() != null) {
            entity.setRiskLevel(RiskLevel.valueOf(dto.getRiskLevel()));
        }

        entity.setRiskScore(dto.getRiskScore());
        entity.setConfidence(dto.getConfidence());
        entity.setExplanation(dto.getExplanation());
        entity.setMainPrediction(dto.isMainPrediction());
        entity.setPredictionTimestamp(dto.getPredictionTimestamp());

        // Risk comparison + warning system
        if (dto.getPreviousRiskLevel() != null) {
            entity.setPreviousRiskLevel(RiskLevel.valueOf(dto.getPreviousRiskLevel()));
        }
        entity.setRiskIncreased(dto.isRiskIncreased());
        entity.setRequiresConfirmation(dto.isRequiresConfirmation());
        entity.setConfirmed(dto.isConfirmed());
        entity.setConfirmedAt(dto.getConfirmedAt());
        entity.setConfirmedBy(dto.getConfirmedBy());
        entity.setModelVersion(dto.getModelVersion());
        entity.setThresholdTriggered(dto.isThresholdTriggered());

        return entity;
    }
}
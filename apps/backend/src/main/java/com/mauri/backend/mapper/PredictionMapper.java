package com.mauri.backend.mapper;

import com.mauri.backend.dto.prediction.PredictionDto;
import com.mauri.backend.entity.Prediction;
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

        return dto;
    }
}
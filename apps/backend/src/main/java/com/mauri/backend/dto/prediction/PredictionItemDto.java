package com.mauri.backend.dto.prediction;

import lombok.Data;

@Data
public class PredictionItemDto {

    private String predictionType;
    private String riskLevel;
    private Number riskScore;
    private Number confidence;
    private String explanation;
    private Boolean isMainPrediction;
}

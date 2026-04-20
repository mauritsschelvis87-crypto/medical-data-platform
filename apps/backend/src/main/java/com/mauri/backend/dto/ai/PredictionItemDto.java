package com.mauri.backend.dto.ai;

import lombok.Data;

@Data
public class PredictionItemDto {

    private String predictionType;
    private String riskLevel;
    private Double riskScore;
    private Double confidence;
    private String explanation;
    private Boolean isMainPrediction;
}
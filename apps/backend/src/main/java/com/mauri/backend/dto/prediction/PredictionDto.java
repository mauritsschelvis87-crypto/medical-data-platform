package com.mauri.backend.dto.prediction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PredictionDto {

    private Long id;

    private String predictionType;
    private String riskLevel;

    private BigDecimal riskScore;
    private BigDecimal confidence;

    private String explanation;

    private boolean mainPrediction;

    private LocalDateTime predictionTimestamp;

    public Long getId() {
        return id;
    }

    public String getPredictionType() {
        return predictionType;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean isMainPrediction() {
        return mainPrediction;
    }

    public LocalDateTime getPredictionTimestamp() {
        return predictionTimestamp;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPredictionType(String predictionType) {
        this.predictionType = predictionType;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public void setMainPrediction(boolean mainPrediction) {
        this.mainPrediction = mainPrediction;
    }

    public void setPredictionTimestamp(LocalDateTime predictionTimestamp) {
        this.predictionTimestamp = predictionTimestamp;
    }
}
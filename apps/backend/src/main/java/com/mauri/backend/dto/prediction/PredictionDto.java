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

    // Risk comparison fields
    private String previousRiskLevel;
    private boolean riskIncreased;

    // Warning system fields
    private boolean requiresConfirmation;
    private boolean confirmed;
    private LocalDateTime confirmedAt;
    private String confirmedBy;

    private String modelVersion;
    private boolean thresholdTriggered;

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

    public String getPreviousRiskLevel() {
        return previousRiskLevel;
    }

    public void setPreviousRiskLevel(String previousRiskLevel) {
        this.previousRiskLevel = previousRiskLevel;
    }

    public boolean isRiskIncreased() {
        return riskIncreased;
    }

    public void setRiskIncreased(boolean riskIncreased) {
        this.riskIncreased = riskIncreased;
    }

    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }

    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public boolean isThresholdTriggered() {
        return thresholdTriggered;
    }

    public void setThresholdTriggered(boolean thresholdTriggered) {
        this.thresholdTriggered = thresholdTriggered;
    }
}
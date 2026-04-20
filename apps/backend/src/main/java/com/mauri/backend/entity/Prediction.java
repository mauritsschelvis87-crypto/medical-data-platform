package com.mauri.backend.entity;

import com.mauri.backend.entity.base.BaseEntity;
import com.mauri.backend.enums.PredictionType;
import com.mauri.backend.enums.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "predictions",
        indexes = {
                @Index(name = "idx_prediction_patient_created_at", columnList = "patient_id, prediction_timestamp"),
                @Index(name = "idx_prediction_patient_type", columnList = "patient_id, prediction_type")
        }
)
public class Prediction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_type", nullable = false, length = 50)
    private PredictionType predictionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "risk_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal riskScore;

    @Column(name = "confidence", precision = 5, scale = 2, nullable = false)
    private BigDecimal confidence;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "is_main_prediction", nullable = false)
    private boolean mainPrediction;

    @Column(name = "triggered_by_reference_id")
    private Long triggeredByReferenceId;

    @Column(name = "prediction_timestamp", nullable = false)
    private LocalDateTime predictionTimestamp;

    // Risk comparison fields
    @Column(name = "previous_risk_level", length = 20)
    @Enumerated(EnumType.STRING)
    private RiskLevel previousRiskLevel;

    @Column(name = "risk_increased", nullable = false)
    private boolean riskIncreased;

    // Warning system fields
    @Column(name = "requires_confirmation", nullable = false)
    private boolean requiresConfirmation;

    @Column(name = "is_confirmed", nullable = false)
    private boolean confirmed;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by")
    private String confirmedBy;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "threshold_triggered", nullable = false)
    private boolean thresholdTriggered;

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PredictionType getPredictionType() {
        return predictionType;
    }

    public void setPredictionType(PredictionType predictionType) {
        this.predictionType = predictionType;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public boolean isMainPrediction() {
        return mainPrediction;
    }

    public void setMainPrediction(boolean mainPrediction) {
        this.mainPrediction = mainPrediction;
    }

    public Long getTriggeredByReferenceId() {
        return triggeredByReferenceId;
    }

    public void setTriggeredByReferenceId(Long triggeredByReferenceId) {
        this.triggeredByReferenceId = triggeredByReferenceId;
    }

    public LocalDateTime getPredictionTimestamp() {
        return predictionTimestamp;
    }

    public void setPredictionTimestamp(LocalDateTime predictionTimestamp) {
        this.predictionTimestamp = predictionTimestamp;
    }

    public RiskLevel getPreviousRiskLevel() {
        return previousRiskLevel;
    }

    public void setPreviousRiskLevel(RiskLevel previousRiskLevel) {
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
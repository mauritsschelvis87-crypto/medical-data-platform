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
}
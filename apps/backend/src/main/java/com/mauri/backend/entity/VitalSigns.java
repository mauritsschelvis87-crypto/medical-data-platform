package com.mauri.backend.entity;

import com.mauri.backend.entity.base.BaseEntity;
import com.mauri.backend.enums.VitalSignSource;
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
        name = "vital_signs",
        indexes = {
                @Index(name = "idx_vital_signs_patient_measured_at", columnList = "patient_id, measured_at")
        }
)
public class VitalSigns extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "blood_pressure_systolic")
    private Integer bloodPressureSystolic;

    @Column(name = "blood_pressure_diastolic")
    private Integer bloodPressureDiastolic;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "glucose", precision = 10, scale = 2)
    private BigDecimal glucose;

    @Column(name = "bmi", precision = 5, scale = 2)
    private BigDecimal bmi;

    @Column(name = "weight", precision = 6, scale = 2)
    private BigDecimal weight;

    @Column(name = "oxygen_saturation", precision = 5, scale = 2)
    private BigDecimal oxygenSaturation;

    @Column(name = "cholesterol", precision = 10, scale = 2)
    private BigDecimal cholesterol;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private VitalSignSource source = VitalSignSource.MANUAL;

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Integer getBloodPressureSystolic() {
        return bloodPressureSystolic;
    }

    public void setBloodPressureSystolic(Integer bloodPressureSystolic) {
        this.bloodPressureSystolic = bloodPressureSystolic;
    }

    public Integer getBloodPressureDiastolic() {
        return bloodPressureDiastolic;
    }

    public void setBloodPressureDiastolic(Integer bloodPressureDiastolic) {
        this.bloodPressureDiastolic = bloodPressureDiastolic;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public BigDecimal getGlucose() {
        return glucose;
    }

    public void setGlucose(BigDecimal glucose) {
        this.glucose = glucose;
    }

    public BigDecimal getBmi() {
        return bmi;
    }

    public void setBmi(BigDecimal bmi) {
        this.bmi = bmi;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getOxygenSaturation() {
        return oxygenSaturation;
    }

    public void setOxygenSaturation(BigDecimal oxygenSaturation) {
        this.oxygenSaturation = oxygenSaturation;
    }

    public BigDecimal getCholesterol() {
        return cholesterol;
    }

    public void setCholesterol(BigDecimal cholesterol) {
        this.cholesterol = cholesterol;
    }

    public LocalDateTime getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(LocalDateTime measuredAt) {
        this.measuredAt = measuredAt;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public VitalSignSource getSource() {
        return source;
    }

    public void setSource(VitalSignSource source) {
        this.source = source;
    }
}
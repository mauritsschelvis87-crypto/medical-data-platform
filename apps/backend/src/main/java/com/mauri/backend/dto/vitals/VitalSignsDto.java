package com.mauri.backend.dto.vitals;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VitalSignsDto {

    private Long id;

    private Integer bloodPressureSystolic;
    private Integer bloodPressureDiastolic;
    private Integer heartRate;

    private BigDecimal temperature;
    private BigDecimal glucose;
    private BigDecimal bmi;
    private BigDecimal weight;
    private BigDecimal oxygenSaturation;
    private BigDecimal cholesterol;

    private LocalDateTime measuredAt;
    private LocalDateTime recordedAt;

    private String source;

    public Long getId() {
        return id;
    }

    public Integer getBloodPressureSystolic() {
        return bloodPressureSystolic;
    }

    public Integer getBloodPressureDiastolic() {
        return bloodPressureDiastolic;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public BigDecimal getGlucose() {
        return glucose;
    }

    public BigDecimal getBmi() {
        return bmi;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getOxygenSaturation() {
        return oxygenSaturation;
    }

    public BigDecimal getCholesterol() {
        return cholesterol;
    }

    public LocalDateTime getMeasuredAt() {
        return measuredAt;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public String getSource() {
        return source;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBloodPressureSystolic(Integer bloodPressureSystolic) {
        this.bloodPressureSystolic = bloodPressureSystolic;
    }

    public void setBloodPressureDiastolic(Integer bloodPressureDiastolic) {
        this.bloodPressureDiastolic = bloodPressureDiastolic;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public void setGlucose(BigDecimal glucose) {
        this.glucose = glucose;
    }

    public void setBmi(BigDecimal bmi) {
        this.bmi = bmi;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public void setOxygenSaturation(BigDecimal oxygenSaturation) {
        this.oxygenSaturation = oxygenSaturation;
    }

    public void setCholesterol(BigDecimal cholesterol) {
        this.cholesterol = cholesterol;
    }

    public void setMeasuredAt(LocalDateTime measuredAt) {
        this.measuredAt = measuredAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
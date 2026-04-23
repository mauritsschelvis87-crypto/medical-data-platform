package com.mauri.backend.dto.vitals;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateVitalSignsRequest {

    @NotBlank
    private String type;

    @NotNull
    private BigDecimal value;

    @NotBlank
    private String unit;

    private LocalDateTime measuredAt;
    private String source;
    private String sourceObservationCode;
    private String sourceDescription;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDateTime getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(LocalDateTime measuredAt) {
        this.measuredAt = measuredAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceObservationCode() {
        return sourceObservationCode;
    }

    public void setSourceObservationCode(String sourceObservationCode) {
        this.sourceObservationCode = sourceObservationCode;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public void setSourceDescription(String sourceDescription) {
        this.sourceDescription = sourceDescription;
    }
}

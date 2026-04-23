package com.mauri.backend.dto.vitals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class VitalSignsDto {

    private UUID id;
    private UUID patientId;
    private String type;
    private String label;
    private BigDecimal value;
    private String unit;
    private LocalDateTime measuredAt;
    private String clinicalStatus;
    private String freshnessStatus;
    private String clinicalMessage;
    private String freshnessMessage;
    private String ageGroup;
    private String interpretationStatus;
    private String interpretationMessage;
    private boolean contextComplete;
    private boolean editable;
    private String source;
    private String sourceObservationCode;
    private String sourceDescription;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public String getClinicalStatus() {
        return clinicalStatus;
    }

    public void setClinicalStatus(String clinicalStatus) {
        this.clinicalStatus = clinicalStatus;
    }

    public String getFreshnessStatus() {
        return freshnessStatus;
    }

    public void setFreshnessStatus(String freshnessStatus) {
        this.freshnessStatus = freshnessStatus;
    }

    public String getClinicalMessage() {
        return clinicalMessage;
    }

    public void setClinicalMessage(String clinicalMessage) {
        this.clinicalMessage = clinicalMessage;
    }

    public String getFreshnessMessage() {
        return freshnessMessage;
    }

    public void setFreshnessMessage(String freshnessMessage) {
        this.freshnessMessage = freshnessMessage;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public void setAgeGroup(String ageGroup) {
        this.ageGroup = ageGroup;
    }

    public String getInterpretationStatus() {
        return interpretationStatus;
    }

    public void setInterpretationStatus(String interpretationStatus) {
        this.interpretationStatus = interpretationStatus;
    }

    public String getInterpretationMessage() {
        return interpretationMessage;
    }

    public void setInterpretationMessage(String interpretationMessage) {
        this.interpretationMessage = interpretationMessage;
    }

    public boolean isContextComplete() {
        return contextComplete;
    }

    public void setContextComplete(boolean contextComplete) {
        this.contextComplete = contextComplete;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
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

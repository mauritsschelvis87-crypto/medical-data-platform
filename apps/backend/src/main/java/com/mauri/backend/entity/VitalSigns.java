package com.mauri.backend.entity;

import com.mauri.backend.entity.base.BaseEntity;
import com.mauri.backend.enums.VitalSignSource;
import com.mauri.backend.enums.VitalSignType;
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
                @Index(name = "idx_vital_signs_patient_measured_at", columnList = "patient_id, measured_at"),
                @Index(name = "idx_vital_signs_patient_type", columnList = "patient_id, type")
        }
)
public class VitalSigns extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private VitalSignType type;

    @Column(name = "measurement_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal value;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @Column(name = "source_observation_code", length = 50)
    private String sourceObservationCode;

    @Column(name = "source_description", length = 255)
    private String sourceDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private VitalSignSource source = VitalSignSource.MANUAL;

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public VitalSignType getType() {
        return type;
    }

    public void setType(VitalSignType type) {
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

    public VitalSignSource getSource() {
        return source;
    }

    public void setSource(VitalSignSource source) {
        this.source = source;
    }
}

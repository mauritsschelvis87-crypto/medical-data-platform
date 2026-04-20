package com.mauri.backend.entity;

import com.mauri.backend.entity.base.BaseEntity;
import com.mauri.backend.enums.MedicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(
        name = "patient_medications",
        indexes = {
                @Index(name = "idx_patient_medication_patient", columnList = "patient_id"),
                @Index(name = "idx_patient_medication_catalog", columnList = "medication_catalog_id")
        }
)
public class PatientMedication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medication_catalog_id", nullable = false)
    private MedicationCatalog medicationCatalog;

    @Column(name = "dosage", nullable = false, length = 100)
    private String dosage;

    @Column(name = "frequency", nullable = false, length = 100)
    private String frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MedicationStatus status = MedicationStatus.ACTIVE;

    @Column(name = "prescribed_by", length = 100)
    private String prescribedBy;

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public MedicationCatalog getMedicationCatalog() {
        return medicationCatalog;
    }

    public void setMedicationCatalog(MedicationCatalog medicationCatalog) {
        this.medicationCatalog = medicationCatalog;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public MedicationStatus getStatus() {
        return status;
    }

    public void setStatus(MedicationStatus status) {
        this.status = status;
    }

    public String getPrescribedBy() {
        return prescribedBy;
    }

    public void setPrescribedBy(String prescribedBy) {
        this.prescribedBy = prescribedBy;
    }
}
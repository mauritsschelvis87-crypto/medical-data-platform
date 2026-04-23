package com.mauri.backend.dto.medication;

import java.time.LocalDate;
import java.util.UUID;

public class CreatePatientMedicationRequest {

    private UUID medicationCatalogId;
    private String dosage;
    private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String prescribedBy;

    public UUID getMedicationCatalogId() {
        return medicationCatalogId;
    }

    public void setMedicationCatalogId(UUID medicationCatalogId) {
        this.medicationCatalogId = medicationCatalogId;
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

    public String getPrescribedBy() {
        return prescribedBy;
    }

    public void setPrescribedBy(String prescribedBy) {
        this.prescribedBy = prescribedBy;
    }
}

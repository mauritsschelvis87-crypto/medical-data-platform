package com.mauri.backend.dto.dataset;

import java.time.LocalDateTime;
import java.util.UUID;

public class DatasetImportDto {

    private UUID id;
    private String sourceName;
    private String datasetType;
    private String status;
    private LocalDateTime importedAt;
    private String notes;
    private Integer recordsReceived;
    private Integer recordsProcessed;
    private Integer recordsFailed;
    private Integer patientCount;
    private Integer patientAddressCount;
    private Integer vitalSignsCount;
    private Integer skippedRecords;
    private String validationSummary;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getRecordsReceived() {
        return recordsReceived;
    }

    public void setRecordsReceived(Integer recordsReceived) {
        this.recordsReceived = recordsReceived;
    }

    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public Integer getRecordsFailed() {
        return recordsFailed;
    }

    public void setRecordsFailed(Integer recordsFailed) {
        this.recordsFailed = recordsFailed;
    }

    public Integer getPatientCount() {
        return patientCount;
    }

    public void setPatientCount(Integer patientCount) {
        this.patientCount = patientCount;
    }

    public Integer getPatientAddressCount() {
        return patientAddressCount;
    }

    public void setPatientAddressCount(Integer patientAddressCount) {
        this.patientAddressCount = patientAddressCount;
    }

    public Integer getVitalSignsCount() {
        return vitalSignsCount;
    }

    public void setVitalSignsCount(Integer vitalSignsCount) {
        this.vitalSignsCount = vitalSignsCount;
    }

    public Integer getSkippedRecords() {
        return skippedRecords;
    }

    public void setSkippedRecords(Integer skippedRecords) {
        this.skippedRecords = skippedRecords;
    }

    public String getValidationSummary() {
        return validationSummary;
    }

    public void setValidationSummary(String validationSummary) {
        this.validationSummary = validationSummary;
    }
}

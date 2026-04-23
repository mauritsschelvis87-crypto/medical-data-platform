package com.mauri.backend.dto.dataset;

import jakarta.validation.constraints.NotBlank;

public class CreateDatasetImportRequest {

    @NotBlank
    private String sourceName;

    @NotBlank
    private String datasetType;

    @NotBlank
    private String sourceDirectoryPath;

    private String notes;
    private boolean replaceExistingData;

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

    public String getSourceDirectoryPath() {
        return sourceDirectoryPath;
    }

    public void setSourceDirectoryPath(String sourceDirectoryPath) {
        this.sourceDirectoryPath = sourceDirectoryPath;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isReplaceExistingData() {
        return replaceExistingData;
    }

    public void setReplaceExistingData(boolean replaceExistingData) {
        this.replaceExistingData = replaceExistingData;
    }
}

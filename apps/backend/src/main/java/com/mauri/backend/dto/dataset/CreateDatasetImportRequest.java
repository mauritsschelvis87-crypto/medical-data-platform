package com.mauri.backend.dto.dataset;

public class CreateDatasetImportRequest {

    private String datasetName;
    private String sourceName;
    private String importType;
    private String normalizationVersion;
    private Integer recordCount;
    private String checksum;
    private String versionTag;

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getImportType() {
        return importType;
    }

    public void setImportType(String importType) {
        this.importType = importType;
    }

    public String getNormalizationVersion() {
        return normalizationVersion;
    }

    public void setNormalizationVersion(String normalizationVersion) {
        this.normalizationVersion = normalizationVersion;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getVersionTag() {
        return versionTag;
    }

    public void setVersionTag(String versionTag) {
        this.versionTag = versionTag;
    }
}

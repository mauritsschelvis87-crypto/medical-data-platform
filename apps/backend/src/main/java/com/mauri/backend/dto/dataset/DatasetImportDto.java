package com.mauri.backend.dto.dataset;

import java.time.LocalDateTime;

public class DatasetImportDto {

    private Long id;
    private String datasetName;
    private String importStatus;

    private Integer recordsReceived;
    private Integer recordsProcessed;
    private Integer recordsFailed;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public Long getId() {
        return id;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getImportStatus() {
        return importStatus;
    }

    public Integer getRecordsReceived() {
        return recordsReceived;
    }

    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public Integer getRecordsFailed() {
        return recordsFailed;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public void setImportStatus(String importStatus) {
        this.importStatus = importStatus;
    }

    public void setRecordsReceived(Integer recordsReceived) {
        this.recordsReceived = recordsReceived;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public void setRecordsFailed(Integer recordsFailed) {
        this.recordsFailed = recordsFailed;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
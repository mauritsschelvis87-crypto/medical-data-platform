package com.mauri.backend.entity;

import com.mauri.backend.entity.base.BaseEntity;
import com.mauri.backend.enums.DatasetImportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "dataset_imports",
        indexes = {
                @Index(name = "idx_dataset_import_dataset_name", columnList = "dataset_name"),
                @Index(name = "idx_dataset_import_status", columnList = "import_status")
        }
)
public class DatasetImport extends BaseEntity {

    @Column(name = "dataset_name", nullable = false, length = 255)
    private String datasetName;

    @Column(name = "source_file_name", nullable = false, length = 255)
    private String sourceFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_status", nullable = false, length = 30)
    private DatasetImportStatus importStatus = DatasetImportStatus.PENDING;

    @Column(name = "records_received")
    private Integer recordsReceived;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "records_failed")
    private Integer recordsFailed;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "checksum", length = 255)
    private String checksum;

    @Column(name = "version_tag", length = 100)
    private String versionTag;

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public DatasetImportStatus getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(DatasetImportStatus importStatus) {
        this.importStatus = importStatus;
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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
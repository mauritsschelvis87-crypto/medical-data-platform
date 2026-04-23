package com.mauri.backend.entity;

import com.mauri.backend.entity.base.BaseEntity;
import com.mauri.backend.enums.DatasetImportStatus;
import com.mauri.backend.enums.DatasetType;
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
                @Index(name = "idx_dataset_import_source_name", columnList = "source_name"),
                @Index(name = "idx_dataset_import_dataset_type", columnList = "dataset_type"),
                @Index(name = "idx_dataset_import_status", columnList = "status")
        }
)
public class DatasetImport extends BaseEntity {

    @Column(name = "source_name", nullable = false, length = 255)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dataset_type", nullable = false, length = 50)
    private DatasetType datasetType;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DatasetImportStatus status = DatasetImportStatus.CREATED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public DatasetType getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(DatasetType datasetType) {
        this.datasetType = datasetType;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }

    public DatasetImportStatus getStatus() {
        return status;
    }

    public void setStatus(DatasetImportStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

package com.mauri.backend.dto.consult;

import java.time.LocalDateTime;

public class ConsultNoteDto {

    private Long id;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;

    private ConsultNoteVersionDto currentVersion;

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ConsultNoteVersionDto getCurrentVersion() {
        return currentVersion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCurrentVersion(ConsultNoteVersionDto currentVersion) {
        this.currentVersion = currentVersion;
    }
}
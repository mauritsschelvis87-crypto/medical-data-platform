package com.mauri.backend.dto.consult;

import java.time.LocalDateTime;
import java.util.UUID;

public class ConsultNoteVersionDto {

    private UUID id;
    private String versionNumber;

    private String subjective;
    private String objective;
    private String assessment;
    private String plan;

    private String createdBy;
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public String getSubjective() {
        return subjective;
    }

    public String getObjective() {
        return objective;
    }

    public String getAssessment() {
        return assessment;
    }

    public String getPlan() {
        return plan;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public void setSubjective(String subjective) {
        this.subjective = subjective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public void setAssessment(String assessment) {
        this.assessment = assessment;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

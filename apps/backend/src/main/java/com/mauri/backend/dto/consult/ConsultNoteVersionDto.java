package com.mauri.backend.dto.consult;

import java.time.LocalDateTime;

public class ConsultNoteVersionDto {

    private Long id;
    private String versionNumber;

    private String subjective;
    private String objective;
    private String assessment;
    private String plan;

    private String createdBy;
    private LocalDateTime createdAt;

    public Long getId() {
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

    public void setId(Long id) {
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
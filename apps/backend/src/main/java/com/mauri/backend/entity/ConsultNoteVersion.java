package com.mauri.backend.entity;

import com.mauri.backend.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "consult_note_versions",
        indexes = {
                @Index(name = "idx_consult_note_version_consult_note", columnList = "consult_note_id")
        }
)
public class ConsultNoteVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consult_note_id", nullable = false)
    private ConsultNote consultNote;

    @Column(name = "version_number", nullable = false, length = 20)
    private String versionNumber;

    @Column(name = "subjective", columnDefinition = "TEXT")
    private String subjective;

    @Column(name = "objective", columnDefinition = "TEXT")
    private String objective;

    @Column(name = "assessment", columnDefinition = "TEXT")
    private String assessment;

    @Column(name = "plan", columnDefinition = "TEXT")
    private String plan;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    public ConsultNote getConsultNote() {
        return consultNote;
    }

    public void setConsultNote(ConsultNote consultNote) {
        this.consultNote = consultNote;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getSubjective() {
        return subjective;
    }

    public void setSubjective(String subjective) {
        this.subjective = subjective;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getAssessment() {
        return assessment;
    }

    public void setAssessment(String assessment) {
        this.assessment = assessment;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
}
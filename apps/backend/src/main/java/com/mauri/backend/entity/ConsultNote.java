package com.mauri.backend.entity;

import com.mauri.backend.entity.base.BaseEntity;
import com.mauri.backend.enums.ConsultNoteStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "consult_notes",
        indexes = {
                @Index(name = "idx_consult_note_patient", columnList = "patient_id")
        }
)
public class ConsultNote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConsultNoteStatus status = ConsultNoteStatus.DRAFT;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "consultNote", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("createdAt ASC")
    private List<ConsultNoteVersion> versions = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private ConsultNoteVersion currentVersion;

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public ConsultNoteStatus getStatus() {
        return status;
    }

    public void setStatus(ConsultNoteStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public List<ConsultNoteVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<ConsultNoteVersion> versions) {
        this.versions = versions;
    }

    public ConsultNoteVersion getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(ConsultNoteVersion currentVersion) {
        this.currentVersion = currentVersion;
    }
}
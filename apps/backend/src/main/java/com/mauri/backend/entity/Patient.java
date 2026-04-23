package com.mauri.backend.entity;

import com.mauri.backend.entity.base.BaseEntity;
import com.mauri.backend.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "patient",
        indexes = {
                @Index(name = "idx_patient_patient_number", columnList = "patient_number"),
                @Index(name = "idx_patient_source_patient_id", columnList = "source_patient_id"),
                @Index(name = "idx_patient_full_name", columnList = "full_name"),
                @Index(name = "idx_patient_birth_date", columnList = "birth_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_patient_patient_number", columnNames = "patient_number"),
                @UniqueConstraint(name = "uk_patient_source_patient_id", columnNames = "source_patient_id")
        }
)
public class Patient extends BaseEntity {

    @Column(name = "patient_number", nullable = false, length = 50)
    private String patientNumber;

    @Column(name = "source_patient_id", nullable = false, length = 100)
    private String sourcePatientId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "full_name", nullable = false, length = 250)
    private String fullName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender = Gender.UNKNOWN;

    @Column(name = "deceased", nullable = false)
    private boolean deceased;

    @Column(name = "death_date")
    private LocalDate deathDate;

    @Column(name = "marital_status", length = 50)
    private String maritalStatus;

    @Column(name = "race", length = 100)
    private String race;

    @Column(name = "ethnicity", length = 100)
    private String ethnicity;

    @OneToOne(mappedBy = "patient", fetch = FetchType.LAZY)
    private PatientAddress address;

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    private List<VitalSigns> vitalSigns = new ArrayList<>();

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    private List<Prediction> predictions = new ArrayList<>();

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    private List<ConsultNote> consultNotes = new ArrayList<>();

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    private List<PatientMedication> medications = new ArrayList<>();

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    private List<TimelineEvent> timelineEvents = new ArrayList<>();

    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    public String getSourcePatientId() {
        return sourcePatientId;
    }

    public void setSourcePatientId(String sourcePatientId) {
        this.sourcePatientId = sourcePatientId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public boolean isDeceased() {
        return deceased;
    }

    public void setDeceased(boolean deceased) {
        this.deceased = deceased;
    }

    public LocalDate getDeathDate() {
        return deathDate;
    }

    public void setDeathDate(LocalDate deathDate) {
        this.deathDate = deathDate;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public String getEthnicity() {
        return ethnicity;
    }

    public void setEthnicity(String ethnicity) {
        this.ethnicity = ethnicity;
    }

    public PatientAddress getAddress() {
        return address;
    }

    public void setAddress(PatientAddress address) {
        this.address = address;
    }

    public List<VitalSigns> getVitalSigns() {
        return vitalSigns;
    }

    public void setVitalSigns(List<VitalSigns> vitalSigns) {
        this.vitalSigns = vitalSigns;
    }

    public List<Prediction> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<Prediction> predictions) {
        this.predictions = predictions;
    }

    public List<ConsultNote> getConsultNotes() {
        return consultNotes;
    }

    public void setConsultNotes(List<ConsultNote> consultNotes) {
        this.consultNotes = consultNotes;
    }

    public List<PatientMedication> getMedications() {
        return medications;
    }

    public void setMedications(List<PatientMedication> medications) {
        this.medications = medications;
    }

    public List<TimelineEvent> getTimelineEvents() {
        return timelineEvents;
    }

    public void setTimelineEvents(List<TimelineEvent> timelineEvents) {
        this.timelineEvents = timelineEvents;
    }
}

package com.mauri.backend.entity;

import com.mauri.backend.entity.base.BaseEntity;
import com.mauri.backend.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "patients",
        indexes = {
                @Index(name = "idx_patient_patient_number", columnList = "patient_number"),
                @Index(name = "idx_patient_last_name", columnList = "last_name"),
                @Index(name = "idx_patient_birth_date", columnList = "birth_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_patient_patient_number", columnNames = "patient_number")
        }
)
public class Patient extends BaseEntity {

    @Column(name = "patient_number", nullable = false, length = 50)
    private String patientNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender = Gender.UNKNOWN;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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
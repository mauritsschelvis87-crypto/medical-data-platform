package com.mauri.backend.dto.patient;

import java.time.LocalDate;

public class PatientSearchResultDto {

    private Long id;
    private String patientNumber;
    private String fullName;
    private LocalDate birthDate;

    public Long getId() {
        return id;
    }

    public String getPatientNumber() {
        return patientNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}
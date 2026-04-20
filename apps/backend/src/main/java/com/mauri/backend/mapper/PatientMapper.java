package com.mauri.backend.mapper;

import com.mauri.backend.dto.patient.PatientDto;
import com.mauri.backend.dto.patient.PatientSearchResultDto;
import com.mauri.backend.entity.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public PatientDto toDto(Patient patient) {
        if (patient == null) {
            return null;
        }

        PatientDto dto = new PatientDto();
        dto.setId(patient.getId());
        dto.setPatientNumber(patient.getPatientNumber());
        dto.setFirstName(patient.getFirstName());
        dto.setLastName(patient.getLastName());
        dto.setBirthDate(patient.getBirthDate());
        dto.setGender(patient.getGender() != null ? patient.getGender().name() : null);
        dto.setPhone(patient.getPhone());
        dto.setEmail(patient.getEmail());
        dto.setAddressLine(patient.getAddressLine());
        dto.setPostalCode(patient.getPostalCode());
        dto.setCity(patient.getCity());
        dto.setCountry(patient.getCountry());

        return dto;
    }

    public PatientSearchResultDto toSearchResultDto(Patient patient) {
        if (patient == null) {
            return null;
        }

        PatientSearchResultDto dto = new PatientSearchResultDto();
        dto.setId(patient.getId());
        dto.setPatientNumber(patient.getPatientNumber());
        dto.setBirthDate(patient.getBirthDate());
        dto.setFullName(buildFullName(patient.getFirstName(), patient.getLastName()));

        return dto;
    }

    private String buildFullName(String firstName, String lastName) {
        String safeFirstName = firstName != null ? firstName.trim() : "";
        String safeLastName = lastName != null ? lastName.trim() : "";

        return (safeFirstName + " " + safeLastName).trim();
    }
}
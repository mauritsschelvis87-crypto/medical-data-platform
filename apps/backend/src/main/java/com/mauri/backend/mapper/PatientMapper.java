package com.mauri.backend.mapper;

import com.mauri.backend.dto.patient.PatientAddressDto;
import com.mauri.backend.dto.patient.PatientDto;
import com.mauri.backend.dto.patient.PatientSearchResultDto;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.PatientAddress;
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
        dto.setSourcePatientId(patient.getSourcePatientId());
        dto.setFirstName(patient.getFirstName());
        dto.setLastName(patient.getLastName());
        dto.setFullName(patient.getFullName());
        dto.setBirthDate(patient.getBirthDate());
        dto.setGender(patient.getGender() != null ? patient.getGender().name() : null);
        dto.setDeceased(patient.isDeceased());
        dto.setDeathDate(patient.getDeathDate());
        dto.setMaritalStatus(patient.getMaritalStatus());
        dto.setRace(patient.getRace());
        dto.setEthnicity(patient.getEthnicity());
        dto.setAddress(toAddressDto(patient.getAddress()));
        return dto;
    }

    public PatientSearchResultDto toSearchResultDto(Patient patient) {
        if (patient == null) {
            return null;
        }

        PatientSearchResultDto dto = new PatientSearchResultDto();
        dto.setId(patient.getId());
        dto.setPatientNumber(patient.getPatientNumber());
        dto.setFullName(patient.getFullName());
        dto.setBirthDate(patient.getBirthDate());
        return dto;
    }

    public PatientAddressDto toAddressDto(PatientAddress address) {
        if (address == null) {
            return null;
        }

        PatientAddressDto dto = new PatientAddressDto();
        dto.setId(address.getId());
        dto.setPatientId(address.getPatient() != null ? address.getPatient().getId() : null);
        dto.setAddressLine(address.getAddressLine());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setCounty(address.getCounty());
        dto.setZipCode(address.getZipCode());
        return dto;
    }
}

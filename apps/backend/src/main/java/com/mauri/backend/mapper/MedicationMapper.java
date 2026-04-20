package com.mauri.backend.mapper;

import com.mauri.backend.dto.medication.MedicationCatalogDto;
import com.mauri.backend.dto.medication.PatientMedicationDto;
import com.mauri.backend.entity.MedicationCatalog;
import com.mauri.backend.entity.PatientMedication;
import org.springframework.stereotype.Component;

@Component
public class MedicationMapper {

    public MedicationCatalogDto toCatalogDto(MedicationCatalog medicationCatalog) {
        if (medicationCatalog == null) {
            return null;
        }

        MedicationCatalogDto dto = new MedicationCatalogDto();
        dto.setId(medicationCatalog.getId());
        dto.setCode(medicationCatalog.getCode());
        dto.setDutchName(medicationCatalog.getDutchName());
        dto.setLatinName(medicationCatalog.getLatinName());
        dto.setDefaultDosage(medicationCatalog.getDefaultDosage());
        dto.setAdvice(medicationCatalog.getAdvice());

        return dto;
    }

    public PatientMedicationDto toPatientMedicationDto(PatientMedication patientMedication) {
        if (patientMedication == null) {
            return null;
        }

        PatientMedicationDto dto = new PatientMedicationDto();
        dto.setId(patientMedication.getId());
        dto.setMedicationName(extractMedicationName(patientMedication));
        dto.setDosage(patientMedication.getDosage());
        dto.setFrequency(patientMedication.getFrequency());
        dto.setStartDate(patientMedication.getStartDate());
        dto.setEndDate(patientMedication.getEndDate());
        dto.setStatus(patientMedication.getStatus() != null ? patientMedication.getStatus().name() : null);

        return dto;
    }

    private String extractMedicationName(PatientMedication patientMedication) {
        if (patientMedication.getMedicationCatalog() == null) {
            return null;
        }

        if (patientMedication.getMedicationCatalog().getDutchName() != null
                && !patientMedication.getMedicationCatalog().getDutchName().isBlank()) {
            return patientMedication.getMedicationCatalog().getDutchName();
        }

        return patientMedication.getMedicationCatalog().getLatinName();
    }
}
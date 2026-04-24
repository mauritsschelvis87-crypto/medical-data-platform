package com.mauri.backend.mapper;

import com.mauri.backend.dto.medication.MedicationCatalogDto;
import com.mauri.backend.dto.medication.MedicationCatalogSearchResultDto;
import com.mauri.backend.dto.medication.MedicationCatalogSelectionDto;
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

    public MedicationCatalogSearchResultDto toSearchResultDto(MedicationCatalog medicationCatalog) {
        if (medicationCatalog == null) {
            return null;
        }

        MedicationCatalogSearchResultDto dto = new MedicationCatalogSearchResultDto();
        dto.setId(medicationCatalog.getId());
        dto.setName(extractCatalogName(medicationCatalog));
        return dto;
    }

    public MedicationCatalogSelectionDto toSelectionDto(MedicationCatalog medicationCatalog,
                                                        String defaultDosage,
                                                        String defaultFrequency) {
        if (medicationCatalog == null) {
            return null;
        }

        MedicationCatalogSelectionDto dto = new MedicationCatalogSelectionDto();
        dto.setId(medicationCatalog.getId());
        dto.setName(extractCatalogName(medicationCatalog));
        dto.setDefaultDosage(defaultDosage);
        dto.setDefaultFrequency(defaultFrequency);
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
        dto.setCreatedAt(patientMedication.getCreatedAt());
        dto.setStatus(patientMedication.getStatus() != null ? patientMedication.getStatus().name() : null);

        return dto;
    }

    private String extractMedicationName(PatientMedication patientMedication) {
        if (patientMedication.getMedicationCatalog() == null) {
            return null;
        }

        return extractCatalogName(patientMedication.getMedicationCatalog());
    }

    public String extractCatalogName(MedicationCatalog medicationCatalog) {
        if (medicationCatalog == null) {
            return null;
        }

        if (medicationCatalog.getDutchName() != null && !medicationCatalog.getDutchName().isBlank()) {
            return medicationCatalog.getDutchName().trim();
        }

        if (medicationCatalog.getLatinName() != null && !medicationCatalog.getLatinName().isBlank()) {
            return medicationCatalog.getLatinName().trim();
        }

        if (medicationCatalog.getCode() != null && !medicationCatalog.getCode().isBlank()) {
            return medicationCatalog.getCode().trim();
        }

        return "Unknown medication";
    }
}

package com.mauri.backend.mapper;

import com.mauri.backend.dto.vitals.VitalSignsDto;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.VitalFreshnessStatus;
import com.mauri.backend.enums.VitalSignType;
import com.mauri.backend.service.VitalSignsStatusService;
import com.mauri.backend.service.interpretation.VitalInterpretationResult;
import com.mauri.backend.service.interpretation.VitalInterpretationService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VitalSignsMapper {

    private final VitalSignsStatusService vitalSignsStatusService;
    private final VitalInterpretationService vitalInterpretationService;

    public VitalSignsMapper(VitalSignsStatusService vitalSignsStatusService,
                            VitalInterpretationService vitalInterpretationService) {
        this.vitalSignsStatusService = vitalSignsStatusService;
        this.vitalInterpretationService = vitalInterpretationService;
    }

    public VitalSignsDto toDto(VitalSigns vitalSigns) {
        return toDto(vitalSigns, vitalSigns != null ? List.of(vitalSigns) : List.of());
    }

    public VitalSignsDto toDto(VitalSigns vitalSigns, List<VitalSigns> contextVitals) {
        if (vitalSigns == null) {
            return null;
        }

        VitalInterpretationResult interpretation = vitalInterpretationService.interpret(vitalSigns, contextVitals);
        VitalFreshnessStatus freshnessStatus = vitalSignsStatusService.resolveFreshnessStatus(vitalSigns.getMeasuredAt());

        VitalSignsDto dto = new VitalSignsDto();
        dto.setId(vitalSigns.getId());
        dto.setPatientId(vitalSigns.getPatient() != null ? vitalSigns.getPatient().getId() : null);
        dto.setType(vitalSigns.getType() != null ? vitalSigns.getType().name() : null);
        dto.setLabel(labelFor(vitalSigns.getType()));
        dto.setValue(vitalSigns.getValue());
        dto.setUnit(vitalSigns.getUnit());
        dto.setMeasuredAt(vitalSigns.getMeasuredAt());

        dto.setClinicalStatus(interpretation.status().name());
        dto.setClinicalMessage(interpretation.message());

        dto.setFreshnessStatus(freshnessStatus.name());
        dto.setFreshnessMessage(vitalSignsStatusService.resolveFreshnessMessage(vitalSigns.getMeasuredAt(), freshnessStatus));

        dto.setAgeGroup(interpretation.ageGroup().name());
        dto.setInterpretationStatus(interpretation.status().name());
        dto.setInterpretationMessage(interpretation.message());
        dto.setContextComplete(interpretation.contextComplete());

        dto.setEditable(vitalSigns.getType() != VitalSignType.BMI);
        dto.setSource(vitalSigns.getSource() != null ? vitalSigns.getSource().name() : null);
        dto.setSourceObservationCode(vitalSigns.getSourceObservationCode());
        dto.setSourceDescription(vitalSigns.getSourceDescription());

        return dto;
    }

    private String labelFor(VitalSignType type) {
        if (type == null) {
            return "Vital sign";
        }

        return switch (type) {
            case BLOOD_PRESSURE_SYSTOLIC -> "Systolic blood pressure";
            case BLOOD_PRESSURE_DIASTOLIC -> "Diastolic blood pressure";
            case HEART_RATE -> "Heart rate";
            case RESPIRATORY_RATE -> "Respiratory rate";
            case BODY_TEMPERATURE -> "Temperature";
            case OXYGEN_SATURATION -> "O2 saturation";
            case WEIGHT -> "Weight";
            case HEIGHT -> "Height";
            case BMI -> "BMI";
            case GLUCOSE -> "Glucose";
            case CHOLESTEROL -> "Cholesterol";
        };
    }
}
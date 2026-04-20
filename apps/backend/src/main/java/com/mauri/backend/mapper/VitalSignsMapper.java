package com.mauri.backend.mapper;

import com.mauri.backend.dto.vitals.VitalSignsDto;
import com.mauri.backend.entity.VitalSigns;
import org.springframework.stereotype.Component;

@Component
public class VitalSignsMapper {

    public VitalSignsDto toDto(VitalSigns vitalSigns) {
        if (vitalSigns == null) {
            return null;
        }

        VitalSignsDto dto = new VitalSignsDto();
        dto.setId(vitalSigns.getId());
        dto.setBloodPressureSystolic(vitalSigns.getBloodPressureSystolic());
        dto.setBloodPressureDiastolic(vitalSigns.getBloodPressureDiastolic());
        dto.setHeartRate(vitalSigns.getHeartRate());
        dto.setTemperature(vitalSigns.getTemperature());
        dto.setGlucose(vitalSigns.getGlucose());
        dto.setBmi(vitalSigns.getBmi());
        dto.setWeight(vitalSigns.getWeight());
        dto.setOxygenSaturation(vitalSigns.getOxygenSaturation());
        dto.setCholesterol(vitalSigns.getCholesterol());
        dto.setMeasuredAt(vitalSigns.getMeasuredAt());
        dto.setRecordedAt(vitalSigns.getRecordedAt());
        dto.setSource(vitalSigns.getSource() != null ? vitalSigns.getSource().name() : null);

        return dto;
    }
}
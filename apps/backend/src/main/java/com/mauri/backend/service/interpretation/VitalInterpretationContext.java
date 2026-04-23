package com.mauri.backend.service.interpretation;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.AgeGroup;
import com.mauri.backend.enums.VitalSignType;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record VitalInterpretationContext(
        Patient patient,
        List<VitalSigns> contextVitals,
        AgeGroup ageGroup,
        LocalDate referenceDate
) {

    public Optional<VitalSigns> latest(VitalSignType type) {
        if (type == null || contextVitals == null) {
            return Optional.empty();
        }
        return contextVitals.stream()
                .filter(vitalSigns -> vitalSigns != null && vitalSigns.getType() == type && vitalSigns.getMeasuredAt() != null)
                .max(Comparator.comparing(VitalSigns::getMeasuredAt));
    }
}

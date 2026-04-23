package com.mauri.backend.service.interpretation;

import com.mauri.backend.enums.AgeGroup;
import com.mauri.backend.enums.VitalClinicalStatus;

public record VitalInterpretationResult(
        VitalClinicalStatus status,
        String message,
        AgeGroup ageGroup,
        boolean contextComplete
) {

    public static VitalInterpretationResult of(VitalClinicalStatus status,
                                               String message,
                                               AgeGroup ageGroup,
                                               boolean contextComplete) {
        return new VitalInterpretationResult(
                status != null ? status : VitalClinicalStatus.UNKNOWN,
                message != null ? message : "Clinical interpretation is unavailable.",
                ageGroup != null ? ageGroup : AgeGroup.UNKNOWN,
                contextComplete
        );
    }
}

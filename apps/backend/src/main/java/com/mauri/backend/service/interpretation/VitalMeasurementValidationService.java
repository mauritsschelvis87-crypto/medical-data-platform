package com.mauri.backend.service.interpretation;

import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.VitalClinicalStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VitalMeasurementValidationService {

    private final VitalInterpretationService vitalInterpretationService;

    public VitalMeasurementValidationService(VitalInterpretationService vitalInterpretationService) {
        this.vitalInterpretationService = vitalInterpretationService;
    }

    public void validateForPersistence(VitalSigns vitalSigns) {
        VitalInterpretationResult interpretation = evaluate(vitalSigns, List.of(vitalSigns));
        if (interpretation.status() == VitalClinicalStatus.OUT_OF_RANGE) {
            throw new IllegalArgumentException(interpretation.message());
        }
    }

    public VitalInterpretationResult evaluate(VitalSigns vitalSigns, List<VitalSigns> contextVitals) {
        List<VitalSigns> effectiveContext = contextVitals == null || contextVitals.isEmpty()
                ? List.of(vitalSigns)
                : contextVitals;
        return vitalInterpretationService.interpret(vitalSigns, effectiveContext);
    }
}

package com.mauri.backend.service.interpretation;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.AgeGroup;
import com.mauri.backend.enums.VitalClinicalStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class VitalInterpretationService {

    private final AgeGroupResolver ageGroupResolver;
    private final List<VitalSignInterpreter> interpreters;

    public VitalInterpretationService(AgeGroupResolver ageGroupResolver,
                                      List<VitalSignInterpreter> interpreters) {
        this.ageGroupResolver = ageGroupResolver;
        this.interpreters = interpreters;
    }

    public VitalInterpretationResult interpret(VitalSigns vitalSigns, List<VitalSigns> contextVitals) {
        if (vitalSigns == null || vitalSigns.getType() == null) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.INSUFFICIENT_CONTEXT,
                    "Vital sign type is required for clinical interpretation.",
                    AgeGroup.UNKNOWN,
                    false
            );
        }

        Patient patient = vitalSigns.getPatient();
        LocalDate referenceDate = vitalSigns.getMeasuredAt() != null ? vitalSigns.getMeasuredAt().toLocalDate() : LocalDate.now();
        AgeGroup ageGroup = ageGroupResolver.resolve(patient, referenceDate);
        VitalInterpretationContext context = new VitalInterpretationContext(
                patient,
                contextVitals != null ? contextVitals : List.of(vitalSigns),
                ageGroup,
                referenceDate
        );

        return interpreters.stream()
                .filter(interpreter -> interpreter.supports(vitalSigns.getType()))
                .findFirst()
                .map(interpreter -> interpreter.interpret(vitalSigns, context))
                .orElseGet(() -> VitalInterpretationResult.of(
                        VitalClinicalStatus.NOT_APPLICABLE,
                        "No clinical interpretation is configured for this vital sign type.",
                        ageGroup,
                        false
                ));
    }
}

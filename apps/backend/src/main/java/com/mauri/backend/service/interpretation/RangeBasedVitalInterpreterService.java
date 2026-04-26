package com.mauri.backend.service.interpretation;

import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.AgeGroup;
import com.mauri.backend.enums.VitalClinicalStatus;
import com.mauri.backend.enums.VitalSignType;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RangeBasedVitalInterpreterService implements VitalSignInterpreter {

    private static final Set<VitalSignType> SUPPORTED_TYPES = Set.of(
            VitalSignType.BLOOD_PRESSURE_SYSTOLIC,
            VitalSignType.BLOOD_PRESSURE_DIASTOLIC,
            VitalSignType.HEART_RATE,
            VitalSignType.RESPIRATORY_RATE,
            VitalSignType.BODY_TEMPERATURE,
            VitalSignType.OXYGEN_SATURATION,
            VitalSignType.GLUCOSE,
            VitalSignType.CHOLESTEROL
    );

    private final VitalReferenceRules vitalReferenceRules;

    public RangeBasedVitalInterpreterService(VitalReferenceRules vitalReferenceRules) {
        this.vitalReferenceRules = vitalReferenceRules;
    }

    @Override
    public boolean supports(VitalSignType type) {
        return SUPPORTED_TYPES.contains(type);
    }

    @Override
    public VitalInterpretationResult interpret(VitalSigns vitalSigns, VitalInterpretationContext context) {
        if (vitalSigns == null || vitalSigns.getType() == null || vitalSigns.getValue() == null) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.INSUFFICIENT_CONTEXT,
                    "Vital sign type and value are required for interpretation.",
                    context.ageGroup(),
                    false
            );
        }
        if (context.ageGroup() == AgeGroup.UNKNOWN) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.INSUFFICIENT_CONTEXT,
                    "Birth date and measurement date are required for age-aware interpretation.",
                    context.ageGroup(),
                    false
            );
        }

        return vitalReferenceRules.findRule(vitalSigns.getType(), context.ageGroup())
                .map(rule -> toResult(vitalSigns, context, rule))
                .orElseGet(() -> VitalInterpretationResult.of(
                        VitalClinicalStatus.INSUFFICIENT_CONTEXT,
                        "No age-specific reference rule is configured for " + label(vitalSigns.getType()) + " in " + context.ageGroup() + ".",
                        context.ageGroup(),
                        false
                ));
    }

    private VitalInterpretationResult toResult(VitalSigns vitalSigns,
                                               VitalInterpretationContext context,
                                               VitalRangeRule rule) {
        VitalClinicalStatus status = rule.evaluate(vitalSigns.getValue());
        boolean contextComplete = status != VitalClinicalStatus.INSUFFICIENT_CONTEXT
                && status != VitalClinicalStatus.OUT_OF_RANGE;
        return VitalInterpretationResult.of(status, message(vitalSigns, context, rule, status), context.ageGroup(), contextComplete);
    }

    private String message(VitalSigns vitalSigns,
                           VitalInterpretationContext context,
                           VitalRangeRule rule,
                           VitalClinicalStatus status) {
        String label = label(vitalSigns.getType());
        return switch (status) {
            case LOW -> label + " is within the conservative configured range for " + context.ageGroup() + ". " + rule.contextNote();
            case MEDIUM -> label + " is outside the expected age-aware range. " + rule.contextNote();
            case HIGH, CRITICAL -> label + " is markedly outside the expected age-aware range. " + rule.contextNote();
            case OUT_OF_RANGE -> label + " is outside a broad plausible measurement range; verify unit, type and source data.";
            case INSUFFICIENT_CONTEXT -> "More patient context is required to interpret " + label + ".";
            case NOT_APPLICABLE -> label + " is not clinically interpreted for this context.";
            case UNKNOWN -> "Clinical interpretation is unavailable for " + label + ".";
            case NEUTRAL -> label + " is within the healthy range for " + context.ageGroup() + ". " + rule.contextNote();
        };
    }

    private String label(VitalSignType type) {
        if (type == null) {
            return "vital sign";
        }
        return type.name().toLowerCase().replace('_', ' ');
    }
}

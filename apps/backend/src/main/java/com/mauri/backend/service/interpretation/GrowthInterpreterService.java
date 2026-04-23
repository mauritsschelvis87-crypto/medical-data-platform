package com.mauri.backend.service.interpretation;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.AgeGroup;
import com.mauri.backend.enums.Gender;
import com.mauri.backend.enums.VitalClinicalStatus;
import com.mauri.backend.enums.VitalSignType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.OptionalInt;

@Service
public class GrowthInterpreterService implements VitalSignInterpreter {

    private final AgeGroupResolver ageGroupResolver;

    public GrowthInterpreterService(AgeGroupResolver ageGroupResolver) {
        this.ageGroupResolver = ageGroupResolver;
    }

    @Override
    public boolean supports(VitalSignType type) {
        return type == VitalSignType.WEIGHT || type == VitalSignType.HEIGHT || type == VitalSignType.BMI;
    }

    @Override
    public VitalInterpretationResult interpret(VitalSigns vitalSigns, VitalInterpretationContext context) {
        if (vitalSigns == null || vitalSigns.getType() == null || vitalSigns.getValue() == null) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.INSUFFICIENT_CONTEXT,
                    "Weight, height or BMI value is required for growth interpretation.",
                    context.ageGroup(),
                    false
            );
        }
        if (context.ageGroup() == AgeGroup.UNKNOWN) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.INSUFFICIENT_CONTEXT,
                    "Birth date and measurement date are required for age-aware growth interpretation.",
                    context.ageGroup(),
                    false
            );
        }

        return switch (vitalSigns.getType()) {
            case WEIGHT -> interpretWeight(vitalSigns, context);
            case HEIGHT -> interpretHeight(vitalSigns, context);
            case BMI -> interpretBmi(vitalSigns, context);
            default -> VitalInterpretationResult.of(
                    VitalClinicalStatus.NOT_APPLICABLE,
                    "This vital sign is not part of the growth domain.",
                    context.ageGroup(),
                    false
            );
        };
    }

    public boolean shouldCalculateBmi(Patient patient, VitalSigns weight, VitalSigns height) {
        if (patient == null || weight == null || height == null || weight.getValue() == null || height.getValue() == null) {
            return false;
        }
        LocalDate referenceDate = referenceDate(weight);
        OptionalInt ageMonths = ageGroupResolver.ageMonths(patient, referenceDate);
        if (ageMonths.isEmpty() || ageMonths.getAsInt() < 24) {
            return false;
        }
        return weight.getValue().signum() > 0
                && height.getValue().signum() > 0
                && measurementsCloseEnough(patient, referenceDate, weight, height);
    }

    public BigDecimal calculateBmi(BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg == null || heightCm == null || heightCm.signum() <= 0) {
            return null;
        }
        BigDecimal heightMeters = heightCm.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        return weightKg.divide(heightMeters.multiply(heightMeters), 1, RoundingMode.HALF_UP);
    }

    private VitalInterpretationResult interpretWeight(VitalSigns weight, VitalInterpretationContext context) {
        VitalInterpretationResult plausibility = plausibleMeasurement(weight, context.ageGroup());
        if (plausibility != null) {
            return plausibility;
        }

        Optional<VitalSigns> height = context.latest(VitalSignType.HEIGHT);
        OptionalInt ageYears = ageGroupResolver.ageYears(context.patient(), context.referenceDate());
        if (height.isEmpty()) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.INSUFFICIENT_CONTEXT,
                    "Weight cannot be interpreted safely without height in the growth domain.",
                    context.ageGroup(),
                    false
            );
        }
        if (!measurementsCloseEnough(context.patient(), context.referenceDate(), weight, height.get())) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.INSUFFICIENT_CONTEXT,
                    "Weight and height measurements are too far apart for reliable combined interpretation.",
                    context.ageGroup(),
                    false
            );
        }
        if (isPediatricBmiAge(ageYears)) {
            return pediatricGrowthContextResult(context, "Weight and height are available, but pediatric growth requires percentile or weight-for-height rules.");
        }

        BigDecimal bmi = calculateBmi(weight.getValue(), height.get().getValue());
        return adultBmiResult(bmi, context.ageGroup(), "Weight is interpreted together with height using adult BMI screening thresholds.");
    }

    private VitalInterpretationResult interpretHeight(VitalSigns height, VitalInterpretationContext context) {
        VitalInterpretationResult plausibility = plausibleMeasurement(height, context.ageGroup());
        if (plausibility != null) {
            return plausibility;
        }

        OptionalInt ageYears = ageGroupResolver.ageYears(context.patient(), context.referenceDate());
        if (isPediatricBmiAge(ageYears)) {
            return pediatricGrowthContextResult(context, "Height is part of pediatric growth assessment and requires growth-chart context.");
        }

        return VitalInterpretationResult.of(
                VitalClinicalStatus.NOT_APPLICABLE,
                "Height alone is not interpreted as a clinical risk signal; it is used with weight for BMI.",
                context.ageGroup(),
                false
        );
    }

    private VitalInterpretationResult interpretBmi(VitalSigns bmiVital, VitalInterpretationContext context) {
        VitalInterpretationResult plausibility = plausibleBmi(bmiVital, context.ageGroup());
        if (plausibility != null) {
            return plausibility;
        }

        OptionalInt ageMonths = ageGroupResolver.ageMonths(context.patient(), context.referenceDate());
        OptionalInt ageYears = ageGroupResolver.ageYears(context.patient(), context.referenceDate());
        if (ageMonths.isPresent() && ageMonths.getAsInt() < 24) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.NOT_APPLICABLE,
                    "BMI is not used for babies under 24 months; weight-for-length/growth-chart context is required.",
                    context.ageGroup(),
                    false
            );
        }
        if (isPediatricBmiAge(ageYears)) {
            return pediatricGrowthContextResult(context, "BMI for patients under 20 requires sex-specific BMI-for-age percentile rules.");
        }

        return adultBmiResult(bmiVital.getValue(), context.ageGroup(), "BMI is interpreted with adult screening thresholds.");
    }

    private VitalInterpretationResult pediatricGrowthContextResult(VitalInterpretationContext context, String baseMessage) {
        if (context.patient() == null || context.patient().getGender() == null || context.patient().getGender() == Gender.UNKNOWN) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.INSUFFICIENT_CONTEXT,
                    baseMessage + " Patient sex is missing or unknown.",
                    context.ageGroup(),
                    false
            );
        }
        return VitalInterpretationResult.of(
                VitalClinicalStatus.INSUFFICIENT_CONTEXT,
                baseMessage + " Adult thresholds are deliberately not applied.",
                context.ageGroup(),
                false
        );
    }

    private VitalInterpretationResult adultBmiResult(BigDecimal bmi, AgeGroup ageGroup, String baseMessage) {
        if (bmi == null) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.INSUFFICIENT_CONTEXT,
                    "BMI requires both weight and height.",
                    ageGroup,
                    false
            );
        }
        if (bmi.compareTo(BigDecimal.valueOf(10)) < 0 || bmi.compareTo(BigDecimal.valueOf(90)) > 0) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.OUT_OF_RANGE,
                    "Calculated BMI is outside a broad plausible range; verify weight, height and units.",
                    ageGroup,
                    false
            );
        }
        VitalClinicalStatus status;
        if (bmi.compareTo(BigDecimal.valueOf(16)) < 0 || bmi.compareTo(BigDecimal.valueOf(35)) >= 0) {
            status = VitalClinicalStatus.HIGH;
        } else if (bmi.compareTo(BigDecimal.valueOf(18.5)) < 0 || bmi.compareTo(BigDecimal.valueOf(25)) >= 0) {
            status = VitalClinicalStatus.MEDIUM;
        } else {
            status = VitalClinicalStatus.LOW;
        }
        return VitalInterpretationResult.of(
                status,
                baseMessage + " BMI=" + bmi.toPlainString() + ".",
                ageGroup,
                true
        );
    }

    private VitalInterpretationResult plausibleMeasurement(VitalSigns vitalSigns, AgeGroup ageGroup) {
        MeasurementRange range = rangeFor(vitalSigns.getType(), ageGroup);
        if (range == null) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.INSUFFICIENT_CONTEXT,
                    "No growth plausibility range is configured for this age group.",
                    ageGroup,
                    false
            );
        }
        if (vitalSigns.getValue().compareTo(range.minimum()) < 0 || vitalSigns.getValue().compareTo(range.maximum()) > 0) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.OUT_OF_RANGE,
                    vitalSigns.getType().name().toLowerCase() + " is outside a broad plausible range for " + ageGroup + "; verify unit and source data.",
                    ageGroup,
                    false
            );
        }
        return null;
    }

    private VitalInterpretationResult plausibleBmi(VitalSigns vitalSigns, AgeGroup ageGroup) {
        if (vitalSigns.getValue().compareTo(BigDecimal.valueOf(8)) < 0 || vitalSigns.getValue().compareTo(BigDecimal.valueOf(90)) > 0) {
            return VitalInterpretationResult.of(
                    VitalClinicalStatus.OUT_OF_RANGE,
                    "BMI is outside a broad plausible range; verify source data and units.",
                    ageGroup,
                    false
            );
        }
        return null;
    }

    private MeasurementRange rangeFor(VitalSignType type, AgeGroup ageGroup) {
        if (type == VitalSignType.WEIGHT) {
            return switch (ageGroup) {
                case BABY -> range(1, 15);
                case TODDLER -> range(5, 30);
                case CHILD -> range(8, 100);
                case ADOLESCENT -> range(25, 200);
                case ADULT, OLDER_ADULT -> range(25, 350);
                case UNKNOWN -> null;
            };
        }
        if (type == VitalSignType.HEIGHT) {
            return switch (ageGroup) {
                case BABY -> range(30, 90);
                case TODDLER -> range(60, 115);
                case CHILD -> range(75, 170);
                case ADOLESCENT -> range(120, 220);
                case ADULT, OLDER_ADULT -> range(120, 230);
                case UNKNOWN -> null;
            };
        }
        return null;
    }

    private boolean measurementsCloseEnough(Patient patient, LocalDate referenceDate, VitalSigns weight, VitalSigns height) {
        if (weight.getMeasuredAt() == null || height.getMeasuredAt() == null) {
            return false;
        }
        long gapDays = Math.abs(ChronoUnit.DAYS.between(weight.getMeasuredAt().toLocalDate(), height.getMeasuredAt().toLocalDate()));
        return gapDays <= maxWeightHeightGapDays(patient, referenceDate);
    }

    private long maxWeightHeightGapDays(Patient patient, LocalDate referenceDate) {
        OptionalInt ageYears = ageGroupResolver.ageYears(patient, referenceDate);
        if (ageYears.isEmpty()) {
            return 0;
        }
        int years = ageYears.getAsInt();
        if (years < 20) {
            return 90;
        }
        if (years >= 65) {
            return 730;
        }
        return 1_825;
    }

    private boolean isPediatricBmiAge(OptionalInt ageYears) {
        return ageYears.isPresent() && ageYears.getAsInt() < 20;
    }

    private LocalDate referenceDate(VitalSigns vitalSigns) {
        return vitalSigns.getMeasuredAt() != null ? vitalSigns.getMeasuredAt().toLocalDate() : LocalDate.now();
    }

    private MeasurementRange range(int minimum, int maximum) {
        return new MeasurementRange(BigDecimal.valueOf(minimum), BigDecimal.valueOf(maximum));
    }

    private record MeasurementRange(BigDecimal minimum, BigDecimal maximum) {
    }
}

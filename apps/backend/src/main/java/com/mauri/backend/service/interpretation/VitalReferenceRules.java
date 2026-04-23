package com.mauri.backend.service.interpretation;

import com.mauri.backend.enums.AgeGroup;
import com.mauri.backend.enums.VitalSignType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class VitalReferenceRules {

    private static final Set<AgeGroup> KNOWN_AGE_GROUPS = Set.of(
            AgeGroup.BABY,
            AgeGroup.TODDLER,
            AgeGroup.CHILD,
            AgeGroup.ADOLESCENT,
            AgeGroup.ADULT,
            AgeGroup.OLDER_ADULT
    );

    private final List<VitalRangeRule> rules = List.of(
            rule(VitalSignType.HEART_RATE, Set.of(AgeGroup.BABY), 40, 80, 100, 160, 205, 240,
                    "Resting heart-rate interpretation depends on age and clinical state."),
            rule(VitalSignType.HEART_RATE, Set.of(AgeGroup.TODDLER), 40, 70, 90, 150, 190, 230,
                    "Resting heart-rate interpretation depends on age and clinical state."),
            rule(VitalSignType.HEART_RATE, Set.of(AgeGroup.CHILD), 35, 55, 70, 120, 160, 220,
                    "Resting heart-rate interpretation depends on age and clinical state."),
            rule(VitalSignType.HEART_RATE, Set.of(AgeGroup.ADOLESCENT), 30, 45, 60, 100, 140, 220,
                    "Resting heart-rate interpretation depends on age and clinical state."),
            rule(VitalSignType.HEART_RATE, Set.of(AgeGroup.ADULT, AgeGroup.OLDER_ADULT), 25, 40, 50, 100, 130, 220,
                    "Resting heart-rate interpretation depends on rhythm, medication and clinical state."),

            rule(VitalSignType.RESPIRATORY_RATE, Set.of(AgeGroup.BABY), 5, 20, 30, 53, 70, 100,
                    "Respiratory-rate interpretation is age-dependent and should be tied to clinical context."),
            rule(VitalSignType.RESPIRATORY_RATE, Set.of(AgeGroup.TODDLER), 5, 15, 22, 37, 55, 90,
                    "Respiratory-rate interpretation is age-dependent and should be tied to clinical context."),
            rule(VitalSignType.RESPIRATORY_RATE, Set.of(AgeGroup.CHILD), 5, 10, 18, 30, 45, 80,
                    "Respiratory-rate interpretation is age-dependent and should be tied to clinical context."),
            rule(VitalSignType.RESPIRATORY_RATE, Set.of(AgeGroup.ADOLESCENT), 4, 8, 12, 20, 35, 70,
                    "Respiratory-rate interpretation is age-dependent and should be tied to clinical context."),
            rule(VitalSignType.RESPIRATORY_RATE, Set.of(AgeGroup.ADULT, AgeGroup.OLDER_ADULT), 4, 8, 12, 20, 30, 70,
                    "Respiratory-rate interpretation is age-dependent and should be tied to clinical context."),

            rule(VitalSignType.BODY_TEMPERATURE, KNOWN_AGE_GROUPS, 30, 35, 36, 37.8, 39.5, 45,
                    "Temperature is interpreted broadly; measurement method is not available."),
            rule(VitalSignType.OXYGEN_SATURATION, KNOWN_AGE_GROUPS, 50.0, 90.0, 94.0, null, null, 100.0,
                    "Oxygen saturation interpretation assumes standard pulse oximetry without altitude or chronic baseline correction."),

            rule(VitalSignType.BLOOD_PRESSURE_SYSTOLIC, Set.of(AgeGroup.BABY), 40, 60, 70, 100, 120, 160,
                    "Pediatric blood pressure requires age, sex, height percentile and repeated readings for precise classification."),
            rule(VitalSignType.BLOOD_PRESSURE_SYSTOLIC, Set.of(AgeGroup.TODDLER), 50, 70, 80, 110, 125, 180,
                    "Pediatric blood pressure requires age, sex, height percentile and repeated readings for precise classification."),
            rule(VitalSignType.BLOOD_PRESSURE_SYSTOLIC, Set.of(AgeGroup.CHILD), 50, 75, 85, 120, 135, 220,
                    "Pediatric blood pressure requires age, sex, height percentile and repeated readings for precise classification."),
            rule(VitalSignType.BLOOD_PRESSURE_SYSTOLIC, Set.of(AgeGroup.ADOLESCENT), 60, 80, 90, 130, 150, 240,
                    "Adolescent blood pressure is interpreted conservatively; repeated readings are needed."),
            rule(VitalSignType.BLOOD_PRESSURE_SYSTOLIC, Set.of(AgeGroup.ADULT, AgeGroup.OLDER_ADULT), 60, 80, 90, 129, 140, 260,
                    "Adult blood pressure interpretation uses a single measurement and should be confirmed clinically."),

            rule(VitalSignType.BLOOD_PRESSURE_DIASTOLIC, Set.of(AgeGroup.BABY), 20, 25, 35, 65, 80, 120,
                    "Pediatric blood pressure requires age, sex, height percentile and repeated readings for precise classification."),
            rule(VitalSignType.BLOOD_PRESSURE_DIASTOLIC, Set.of(AgeGroup.TODDLER), 20, 30, 40, 70, 85, 130,
                    "Pediatric blood pressure requires age, sex, height percentile and repeated readings for precise classification."),
            rule(VitalSignType.BLOOD_PRESSURE_DIASTOLIC, Set.of(AgeGroup.CHILD), 20, 35, 45, 80, 95, 140,
                    "Pediatric blood pressure requires age, sex, height percentile and repeated readings for precise classification."),
            rule(VitalSignType.BLOOD_PRESSURE_DIASTOLIC, Set.of(AgeGroup.ADOLESCENT), 20, 40, 50, 85, 100, 150,
                    "Adolescent blood pressure is interpreted conservatively; repeated readings are needed."),
            rule(VitalSignType.BLOOD_PRESSURE_DIASTOLIC, Set.of(AgeGroup.ADULT, AgeGroup.OLDER_ADULT), 20, 50, 60, 79, 90, 160,
                    "Adult blood pressure interpretation uses a single measurement and should be confirmed clinically."),

            rule(VitalSignType.GLUCOSE, KNOWN_AGE_GROUPS, 0.5, 3.0, 3.9, 7.0, 11.1, 40.0,
                    "Glucose interpretation is conservative because fasting/random sample context is unavailable."),
            rule(VitalSignType.CHOLESTEROL, Set.of(AgeGroup.CHILD, AgeGroup.ADOLESCENT), 1.0, null, null, 4.4, 5.2, 15.0,
                    "Pediatric cholesterol interpretation assumes total cholesterol; lipid fraction and fasting context are unavailable."),
            rule(VitalSignType.CHOLESTEROL, Set.of(AgeGroup.ADULT, AgeGroup.OLDER_ADULT), 1.0, null, null, 5.0, 6.5, 20.0,
                    "Adult cholesterol interpretation assumes total cholesterol; lipid fraction and fasting context are unavailable.")
    );

    public Optional<VitalRangeRule> findRule(VitalSignType type, AgeGroup ageGroup) {
        if (type == null || ageGroup == null) {
            return Optional.empty();
        }
        return rules.stream()
                .filter(rule -> rule.supports(type, ageGroup))
                .findFirst();
    }

    private VitalRangeRule rule(VitalSignType type,
                                Set<AgeGroup> ageGroups,
                                double plausibleMinimum,
                                double criticalMinimum,
                                double expectedMinimum,
                                double expectedMaximum,
                                double criticalMaximum,
                                double plausibleMaximum,
                                String contextNote) {
        return rule(
                type,
                ageGroups,
                plausibleMinimum,
                Double.valueOf(criticalMinimum),
                Double.valueOf(expectedMinimum),
                Double.valueOf(expectedMaximum),
                Double.valueOf(criticalMaximum),
                plausibleMaximum,
                contextNote
        );
    }

    private VitalRangeRule rule(VitalSignType type,
                                Set<AgeGroup> ageGroups,
                                double plausibleMinimum,
                                Double criticalMinimum,
                                Double expectedMinimum,
                                Double expectedMaximum,
                                Double criticalMaximum,
                                double plausibleMaximum,
                                String contextNote) {
        return new VitalRangeRule(
                type,
                ageGroups,
                decimal(plausibleMinimum),
                decimal(criticalMinimum),
                decimal(expectedMinimum),
                decimal(expectedMaximum),
                decimal(criticalMaximum),
                decimal(plausibleMaximum),
                contextNote
        );
    }

    private BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}

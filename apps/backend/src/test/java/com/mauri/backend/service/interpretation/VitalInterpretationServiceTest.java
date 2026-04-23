package com.mauri.backend.service.interpretation;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.Gender;
import com.mauri.backend.enums.VitalClinicalStatus;
import com.mauri.backend.enums.VitalSignType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VitalInterpretationServiceTest {

    private VitalInterpretationService vitalInterpretationService;

    @BeforeEach
    void setUp() {
        AgeGroupResolver ageGroupResolver = new AgeGroupResolver();
        GrowthInterpreterService growthInterpreterService = new GrowthInterpreterService(ageGroupResolver);
        RangeBasedVitalInterpreterService rangeBasedVitalInterpreterService =
                new RangeBasedVitalInterpreterService(new VitalReferenceRules());
        vitalInterpretationService = new VitalInterpretationService(
                ageGroupResolver,
                List.of(growthInterpreterService, rangeBasedVitalInterpreterService)
        );
    }

    @Test
    void interpretsAdultWeightThroughGrowthDomain() {
        Patient patient = patient(LocalDate.of(1985, 5, 10), Gender.MALE);
        VitalSigns weight = vital(patient, VitalSignType.WEIGHT, "80.0", LocalDateTime.of(2026, 4, 20, 10, 0));
        VitalSigns height = vital(patient, VitalSignType.HEIGHT, "180.0", LocalDateTime.of(2026, 4, 20, 10, 0));

        VitalInterpretationResult result = vitalInterpretationService.interpret(weight, List.of(weight, height));

        assertEquals(VitalClinicalStatus.LOW, result.status());
    }

    @Test
    void marksPediatricWeightAsInsufficientWithoutPercentileRules() {
        Patient patient = patient(LocalDate.of(2018, 3, 12), Gender.FEMALE);
        VitalSigns weight = vital(patient, VitalSignType.WEIGHT, "25.0", LocalDateTime.of(2026, 4, 20, 10, 0));
        VitalSigns height = vital(patient, VitalSignType.HEIGHT, "120.0", LocalDateTime.of(2026, 4, 10, 10, 0));

        VitalInterpretationResult result = vitalInterpretationService.interpret(weight, List.of(weight, height));

        assertEquals(VitalClinicalStatus.INSUFFICIENT_CONTEXT, result.status());
    }

    @Test
    void marksInfantBmiAsNotApplicable() {
        Patient patient = patient(LocalDate.of(2025, 8, 1), Gender.FEMALE);
        VitalSigns bmi = vital(patient, VitalSignType.BMI, "17.0", LocalDateTime.of(2026, 4, 20, 10, 0));

        VitalInterpretationResult result = vitalInterpretationService.interpret(bmi, List.of(bmi));

        assertEquals(VitalClinicalStatus.NOT_APPLICABLE, result.status());
    }

    @Test
    void usesAgeAwareHeartRateRuleForBabies() {
        Patient patient = patient(LocalDate.of(2025, 9, 1), Gender.MALE);
        VitalSigns heartRate = vital(patient, VitalSignType.HEART_RATE, "95.0", LocalDateTime.of(2026, 4, 20, 10, 0));

        VitalInterpretationResult result = vitalInterpretationService.interpret(heartRate, List.of(heartRate));

        assertEquals(VitalClinicalStatus.MEDIUM, result.status());
    }

    private Patient patient(LocalDate birthDate, Gender gender) {
        Patient patient = new Patient();
        patient.setBirthDate(birthDate);
        patient.setGender(gender);
        return patient;
    }

    private VitalSigns vital(Patient patient, VitalSignType type, String value, LocalDateTime measuredAt) {
        VitalSigns vitalSigns = new VitalSigns();
        vitalSigns.setPatient(patient);
        vitalSigns.setType(type);
        vitalSigns.setValue(new BigDecimal(value));
        vitalSigns.setMeasuredAt(measuredAt);
        vitalSigns.setUnit(unit(type));
        return vitalSigns;
    }

    private String unit(VitalSignType type) {
        return switch (type) {
            case WEIGHT -> "kg";
            case HEIGHT -> "cm";
            case BMI -> "kg/m2";
            case HEART_RATE -> "bpm";
            default -> "unit";
        };
    }
}

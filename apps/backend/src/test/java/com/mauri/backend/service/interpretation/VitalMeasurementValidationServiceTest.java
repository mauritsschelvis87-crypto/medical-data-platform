package com.mauri.backend.service.interpretation;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.Gender;
import com.mauri.backend.enums.VitalSignType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VitalMeasurementValidationServiceTest {

    private VitalMeasurementValidationService validationService;

    @BeforeEach
    void setUp() {
        AgeGroupResolver ageGroupResolver = new AgeGroupResolver();
        GrowthInterpreterService growthInterpreterService = new GrowthInterpreterService(ageGroupResolver);
        RangeBasedVitalInterpreterService rangeBasedVitalInterpreterService =
                new RangeBasedVitalInterpreterService(new VitalReferenceRules());
        VitalInterpretationService vitalInterpretationService = new VitalInterpretationService(
                ageGroupResolver,
                List.of(growthInterpreterService, rangeBasedVitalInterpreterService)
        );
        validationService = new VitalMeasurementValidationService(vitalInterpretationService);
    }

    @Test
    void rejectsImplausibleTemperatureForPersistence() {
        Patient patient = patient(LocalDate.of(1988, 6, 10), Gender.FEMALE);
        VitalSigns temperature = vital(patient, VitalSignType.BODY_TEMPERATURE, "20.0", "C");

        assertThrows(IllegalArgumentException.class, () -> validationService.validateForPersistence(temperature));
    }

    @Test
    void allowsNormalAdultTemperatureForPersistence() {
        Patient patient = patient(LocalDate.of(1988, 6, 10), Gender.FEMALE);
        VitalSigns temperature = vital(patient, VitalSignType.BODY_TEMPERATURE, "36.5", "C");

        assertDoesNotThrow(() -> validationService.validateForPersistence(temperature));
    }

    private Patient patient(LocalDate birthDate, Gender gender) {
        Patient patient = new Patient();
        patient.setBirthDate(birthDate);
        patient.setGender(gender);
        return patient;
    }

    private VitalSigns vital(Patient patient, VitalSignType type, String value, String unit) {
        VitalSigns vitalSigns = new VitalSigns();
        vitalSigns.setPatient(patient);
        vitalSigns.setType(type);
        vitalSigns.setValue(new BigDecimal(value));
        vitalSigns.setMeasuredAt(LocalDateTime.of(2026, 4, 23, 12, 0));
        vitalSigns.setUnit(unit);
        return vitalSigns;
    }
}

package com.mauri.backend.service.interpretation;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.enums.AgeGroup;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.OptionalInt;

@Service
public class AgeGroupResolver {

    public AgeGroup resolve(Patient patient, LocalDate referenceDate) {
        if (patient == null || patient.getBirthDate() == null || referenceDate == null) {
            return AgeGroup.UNKNOWN;
        }

        if (patient.getBirthDate().isAfter(referenceDate)) {
            return AgeGroup.UNKNOWN;
        }

        int years = Period.between(patient.getBirthDate(), referenceDate).getYears();

        if (years < 1) {
            return AgeGroup.BABY;
        }

        if (years < 3) {
            return AgeGroup.TODDLER;
        }

        if (years < 12) {
            return AgeGroup.CHILD;
        }

        if (years < 18) {
            return AgeGroup.ADOLESCENT;
        }

        if (years < 65) {
            return AgeGroup.ADULT;
        }

        return AgeGroup.OLDER_ADULT;
    }

    public OptionalInt ageYears(Patient patient, LocalDate referenceDate) {
        if (patient == null || patient.getBirthDate() == null || referenceDate == null || patient.getBirthDate().isAfter(referenceDate)) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(Period.between(patient.getBirthDate(), referenceDate).getYears());
    }

    public OptionalInt ageMonths(Patient patient, LocalDate referenceDate) {
        if (patient == null || patient.getBirthDate() == null || referenceDate == null || patient.getBirthDate().isAfter(referenceDate)) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(Math.toIntExact(ChronoUnit.MONTHS.between(patient.getBirthDate(), referenceDate)));
    }
}
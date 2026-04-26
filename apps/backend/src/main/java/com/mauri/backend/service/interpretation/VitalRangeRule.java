package com.mauri.backend.service.interpretation;

import com.mauri.backend.enums.AgeGroup;
import com.mauri.backend.enums.VitalClinicalStatus;
import com.mauri.backend.enums.VitalSignType;

import java.math.BigDecimal;
import java.util.Set;

public record VitalRangeRule(
        VitalSignType type,
        Set<AgeGroup> ageGroups,
        BigDecimal plausibleMinimum,
        BigDecimal criticalMinimum,
        BigDecimal expectedMinimum,
        BigDecimal expectedMaximum,
        BigDecimal criticalMaximum,
        BigDecimal plausibleMaximum,
        String contextNote
) {

    public boolean supports(VitalSignType vitalType, AgeGroup ageGroup) {
        return type == vitalType && ageGroups.contains(ageGroup);
    }

    public VitalClinicalStatus evaluate(BigDecimal value) {
        if (value == null) {
            return VitalClinicalStatus.INSUFFICIENT_CONTEXT;
        }

        if (isBelow(value, plausibleMinimum) || isAbove(value, plausibleMaximum)) {
            return VitalClinicalStatus.OUT_OF_RANGE;
        }

        if (isBelow(value, criticalMinimum) || isAbove(value, criticalMaximum)) {
            return VitalClinicalStatus.HIGH;
        }

        if (isModeratelyOutsideExpectedRange(value)) {
            return VitalClinicalStatus.MEDIUM;
        }

        if (isBelow(value, expectedMinimum) || isAbove(value, expectedMaximum)) {
            return VitalClinicalStatus.LOW;
        }

        return VitalClinicalStatus.NEUTRAL;
    }

    private boolean isModeratelyOutsideExpectedRange(BigDecimal value) {
        if (expectedMinimum != null && criticalMinimum != null && value.compareTo(expectedMinimum) < 0) {
            BigDecimal midpoint = expectedMinimum.add(criticalMinimum).divide(BigDecimal.valueOf(2));
            return value.compareTo(midpoint) <= 0;
        }

        if (expectedMaximum != null && criticalMaximum != null && value.compareTo(expectedMaximum) > 0) {
            BigDecimal midpoint = expectedMaximum.add(criticalMaximum).divide(BigDecimal.valueOf(2));
            return value.compareTo(midpoint) >= 0;
        }

        return false;
    }

    private boolean isBelow(BigDecimal value, BigDecimal threshold) {
        return threshold != null && value.compareTo(threshold) < 0;
    }

    private boolean isAbove(BigDecimal value, BigDecimal threshold) {
        return threshold != null && value.compareTo(threshold) > 0;
    }
}
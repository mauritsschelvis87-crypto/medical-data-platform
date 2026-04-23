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
        if (isBelow(value, expectedMinimum) || isAbove(value, expectedMaximum)) {
            return VitalClinicalStatus.MEDIUM;
        }
        return VitalClinicalStatus.LOW;
    }

    private boolean isBelow(BigDecimal value, BigDecimal threshold) {
        return threshold != null && value.compareTo(threshold) < 0;
    }

    private boolean isAbove(BigDecimal value, BigDecimal threshold) {
        return threshold != null && value.compareTo(threshold) > 0;
    }
}

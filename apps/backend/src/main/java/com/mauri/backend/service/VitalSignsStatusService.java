package com.mauri.backend.service;

import com.mauri.backend.enums.VitalFreshnessStatus;
import com.mauri.backend.service.interpretation.GrowthInterpreterService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class VitalSignsStatusService {

    private static final long CURRENT_DAYS = 90;

    private final GrowthInterpreterService growthInterpreterService;

    public VitalSignsStatusService(GrowthInterpreterService growthInterpreterService) {
        this.growthInterpreterService = growthInterpreterService;
    }

    public VitalFreshnessStatus resolveFreshnessStatus(LocalDateTime measuredAt) {
        if (measuredAt == null) {
            return VitalFreshnessStatus.UNKNOWN;
        }

        long ageDays = Duration.between(measuredAt, LocalDateTime.now()).toDays();
        return ageDays <= CURRENT_DAYS ? VitalFreshnessStatus.CURRENT : VitalFreshnessStatus.OUTDATED;
    }

    public String resolveFreshnessMessage(LocalDateTime measuredAt, VitalFreshnessStatus status) {
        if (status == VitalFreshnessStatus.UNKNOWN || measuredAt == null) {
            return "No measurement date available.";
        }

        long ageDays = Math.max(0, Duration.between(measuredAt, LocalDateTime.now()).toDays());
        return switch (status) {
            case CURRENT -> "Measurement is current.";
            case OUTDATED -> "Measurement is outdated.";
            case AGING -> "Measurement is treated as outdated by the current two-state freshness policy.";
            case UNKNOWN -> "No measurement date available.";
        } + " Age: " + ageDays + " days.";
    }

    public BigDecimal calculateBmi(BigDecimal weightKg, BigDecimal heightCm) {
        return growthInterpreterService.calculateBmi(weightKg, heightCm);
    }
}

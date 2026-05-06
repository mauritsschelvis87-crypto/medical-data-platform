package com.mauri.backend.service;

import com.mauri.backend.dto.prediction.PredictionDto;
import com.mauri.backend.dto.prediction.PredictionItemDto;
import com.mauri.backend.dto.prediction.PredictionRequestDto;
import com.mauri.backend.dto.prediction.PredictionResponseDto;
import com.mauri.backend.entity.ConsultNote;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.PatientMedication;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.AgeGroup;
import com.mauri.backend.enums.MedicationStatus;
import com.mauri.backend.enums.PredictionType;
import com.mauri.backend.enums.RiskLevel;
import com.mauri.backend.enums.VitalClinicalStatus;
import com.mauri.backend.enums.VitalSignType;
import com.mauri.backend.repository.ConsultNoteRepository;
import com.mauri.backend.repository.PatientMedicationRepository;
import com.mauri.backend.repository.VitalSignsRepository;
import com.mauri.backend.service.ai.AiService;
import com.mauri.backend.service.interpretation.AgeGroupResolver;
import com.mauri.backend.service.interpretation.VitalInterpretationResult;
import com.mauri.backend.service.interpretation.VitalInterpretationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PredictionWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(PredictionWorkflowService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PatientService patientService;
    private final VitalSignsRepository vitalSignsRepository;
    private final PatientMedicationRepository patientMedicationRepository;
    private final ConsultNoteRepository consultNoteRepository;
    private final PredictionService predictionService;
    private final AiService aiService;
    private final AgeGroupResolver ageGroupResolver;
    private final VitalInterpretationService vitalInterpretationService;

    public PredictionWorkflowService(PatientService patientService,
                                     VitalSignsRepository vitalSignsRepository,
                                     PatientMedicationRepository patientMedicationRepository,
                                     ConsultNoteRepository consultNoteRepository,
                                     PredictionService predictionService,
                                     AiService aiService,
                                     AgeGroupResolver ageGroupResolver,
                                     VitalInterpretationService vitalInterpretationService) {
        this.patientService = patientService;
        this.vitalSignsRepository = vitalSignsRepository;
        this.patientMedicationRepository = patientMedicationRepository;
        this.consultNoteRepository = consultNoteRepository;
        this.predictionService = predictionService;
        this.aiService = aiService;
        this.ageGroupResolver = ageGroupResolver;
        this.vitalInterpretationService = vitalInterpretationService;
    }

    @Transactional
    public List<PredictionDto> recalculatePredictions(UUID patientId, String triggerSource, UUID triggeredByReferenceId) {
        Patient patient = patientService.getPatientEntityById(patientId);
        PredictionRequestDto request = buildRequest(patient, triggerSource);
        List<PredictionType> requestedTypes = requestedPredictionTypes(request);
        Map<PredictionType, PredictionResolution> resolvedPredictions = resolvePredictions(request, requestedTypes);

        List<PredictionDto> savedPredictions = new ArrayList<>();

        if (resolvedPredictions.isEmpty()) {
            return savedPredictions;
        }

        for (int index = 0; index < requestedTypes.size(); index++) {
            PredictionType predictionType = requestedTypes.get(index);
            PredictionResolution resolution = resolvedPredictions.get(predictionType);

            if (resolution == null || resolution.item() == null) {
                continue;
            }

            PredictionItemDto item = resolution.item();
            double score = toDouble(item.getRiskScore());
            RiskLevel riskLevel = resolveRiskLevel(score);

            PredictionDto predictionDto = new PredictionDto();
            predictionDto.setPredictionType(predictionType.name());
            predictionDto.setRiskLevel(riskLevel.name());
            predictionDto.setRiskScore(toBigDecimal(score));
            predictionDto.setConfidence(toBigDecimal(item.getConfidence()));
            predictionDto.setExplanation(resolveExplanation(item.getExplanation()));
            predictionDto.setMainPrediction(index == 0);
            predictionDto.setModelVersion(resolution.modelVersion());

            savedPredictions.add(predictionService.savePrediction(patientId, predictionDto, triggeredByReferenceId));
        }

        return savedPredictions;
    }

    private PredictionRequestDto buildRequest(Patient patient, String triggerSource) {
        PredictionRequestDto request = new PredictionRequestDto();
        request.setPatientId(patient.getId().toString());
        request.setTriggerSource(triggerSource);
        request.setPredictionTypes(EnumSet.allOf(PredictionType.class).stream().map(Enum::name).toList());
        request.setFeatures(buildFeatures(patient));
        return request;
    }

    private Map<String, Object> buildFeatures(Patient patient) {
        Map<String, Object> features = new HashMap<>();
        AgeGroup currentAgeGroup = ageGroupResolver.resolve(patient, LocalDate.now());

        features.put("patientNumber", patient.getPatientNumber());
        features.put("birthDate", patient.getBirthDate() != null ? patient.getBirthDate().format(DATE_FORMATTER) : null);
        features.put("age", patient.getBirthDate() != null ? Period.between(patient.getBirthDate(), LocalDate.now()).getYears() : null);
        features.put("ageGroup", currentAgeGroup.name());
        features.put("gender", patient.getGender() != null ? patient.getGender().name() : null);

        List<VitalSigns> recentVitals = vitalSignsRepository.findLatestPerTypeByPatient(patient);
        Map<VitalSignType, VitalSigns> latestByType = new HashMap<>();
        Map<String, Object> vitalInterpretations = new LinkedHashMap<>();

        for (VitalSigns vitalSigns : recentVitals) {
            if (vitalSigns == null || vitalSigns.getType() == null) {
                continue;
            }

            latestByType.putIfAbsent(vitalSigns.getType(), vitalSigns);

            VitalInterpretationResult interpretation = vitalInterpretationService.interpret(vitalSigns, recentVitals);
            vitalInterpretations.put(vitalSigns.getType().name(), interpretationPayload(interpretation));
        }

        features.put("bloodPressureSystolic", numericValueIfUsable(latestByType.get(VitalSignType.BLOOD_PRESSURE_SYSTOLIC), vitalInterpretations, VitalSignType.BLOOD_PRESSURE_SYSTOLIC));
        features.put("bloodPressureDiastolic", numericValueIfUsable(latestByType.get(VitalSignType.BLOOD_PRESSURE_DIASTOLIC), vitalInterpretations, VitalSignType.BLOOD_PRESSURE_DIASTOLIC));
        features.put("heartRate", numericValueIfUsable(latestByType.get(VitalSignType.HEART_RATE), vitalInterpretations, VitalSignType.HEART_RATE));
        features.put("temperature", numericValueIfUsable(latestByType.get(VitalSignType.BODY_TEMPERATURE), vitalInterpretations, VitalSignType.BODY_TEMPERATURE));
        features.put("glucose", numericValueIfUsable(latestByType.get(VitalSignType.GLUCOSE), vitalInterpretations, VitalSignType.GLUCOSE));
        features.put("bmi", numericValueIfUsable(latestByType.get(VitalSignType.BMI), vitalInterpretations, VitalSignType.BMI));
        features.put("weight", numericValueIfUsable(latestByType.get(VitalSignType.WEIGHT), vitalInterpretations, VitalSignType.WEIGHT));
        features.put("height", numericValueIfUsable(latestByType.get(VitalSignType.HEIGHT), vitalInterpretations, VitalSignType.HEIGHT));
        features.put("oxygenSaturation", numericValueIfUsable(latestByType.get(VitalSignType.OXYGEN_SATURATION), vitalInterpretations, VitalSignType.OXYGEN_SATURATION));
        features.put("cholesterol", numericValueIfUsable(latestByType.get(VitalSignType.CHOLESTEROL), vitalInterpretations, VitalSignType.CHOLESTEROL));
        features.put("measuredAt", latestMeasurementTimestamp(recentVitals));
        features.put("vitalInterpretations", vitalInterpretations);

        List<PatientMedication> activeMedications = patientMedicationRepository.findByPatientAndStatusInOrderByStartDateDesc(
                patient,
                List.of(MedicationStatus.ACTIVE)
        );
        features.put("activeMedicationCount", activeMedications.size());

        List<ConsultNote> recentNotes = consultNoteRepository.findTop5ByPatientOrderByCreatedAtDesc(patient);
        features.put("recentConsultCount", recentNotes.size());

        return features;
    }

    private Map<PredictionType, PredictionResolution> resolvePredictions(PredictionRequestDto request,
                                                                         List<PredictionType> requestedTypes) {
        Map<PredictionType, PredictionResolution> resolvedPredictions = new LinkedHashMap<>();
        Map<PredictionType, PredictionItemDto> fallbackPredictions = buildFallbackPredictions(request.getFeatures(), requestedTypes);
        Map<PredictionType, PredictionItemDto> aiPredictions = loadAiPredictions(request);

        for (PredictionType predictionType : requestedTypes) {
            PredictionItemDto aiItem = aiPredictions.get(predictionType);

            if (aiItem != null) {
                resolvedPredictions.put(predictionType, new PredictionResolution(aiItem, "ai-service"));
                continue;
            }

            PredictionItemDto fallbackItem = fallbackPredictions.get(predictionType);

            if (fallbackItem != null) {
                resolvedPredictions.put(predictionType, new PredictionResolution(fallbackItem, "backend-fallback"));
            }
        }

        return resolvedPredictions;
    }

    private Map<PredictionType, PredictionItemDto> loadAiPredictions(PredictionRequestDto request) {
        PredictionResponseDto response;

        try {
            response = aiService.calculatePredictions(request);
        } catch (Exception exception) {
            log.warn("AI service unavailable. Using backend fallback predictions.");
            return Map.of();
        }

        if (response == null || response.getPredictions() == null || response.getPredictions().isEmpty()) {
            return Map.of();
        }

        Map<PredictionType, PredictionItemDto> aiPredictions = new LinkedHashMap<>();

        for (PredictionItemDto item : response.getPredictions()) {
            if (!isUsableAiItem(item)) {
                continue;
            }

            try {
                PredictionType type = PredictionType.valueOf(item.getPredictionType().trim().toUpperCase(Locale.ROOT));
                aiPredictions.put(type, item);
            } catch (Exception ignored) {
            }
        }

        return aiPredictions;
    }

    private boolean isUsableAiItem(PredictionItemDto item) {
        return item != null
                && item.getPredictionType() != null
                && item.getRiskScore() != null;
    }

    private Map<String, Object> interpretationPayload(VitalInterpretationResult interpretation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", interpretation.status().name());
        payload.put("message", interpretation.message());
        return payload;
    }

    private String latestMeasurementTimestamp(List<VitalSigns> recentVitals) {
        return recentVitals.stream()
                .map(VitalSigns::getMeasuredAt)
                .filter(measuredAt -> measuredAt != null)
                .max(Comparator.naturalOrder())
                .map(LocalDateTime::toString)
                .orElse(null);
    }

    private List<PredictionType> requestedPredictionTypes(PredictionRequestDto request) {
        if (request == null || request.getPredictionTypes() == null || request.getPredictionTypes().isEmpty()) {
            return List.of();
        }

        return request.getPredictionTypes()
                .stream()
                .map(type -> PredictionType.valueOf(type.trim().toUpperCase(Locale.ROOT)))
                .toList();
    }

    private Map<PredictionType, PredictionItemDto> buildFallbackPredictions(Map<String, Object> features,
                                                                            List<PredictionType> predictionTypes) {
        Map<PredictionType, PredictionItemDto> predictions = new LinkedHashMap<>();

        for (PredictionType predictionType : predictionTypes) {
            predictions.put(predictionType, buildFallbackPrediction(predictionType, features));
        }

        return predictions;
    }

    private PredictionItemDto buildFallbackPrediction(PredictionType predictionType, Map<String, Object> features) {
        double score = switch (predictionType) {
            case CARDIOVASCULAR_RISK -> calculateCardiovascularScore(features);
            case DIABETES_RISK -> calculateDiabetesScore(features);
            case GENERAL_DETERIORATION -> calculateGeneralScore(features);
            case SEPSIS_RISK -> calculateSepsisScore(features);
            case RESPIRATORY_RISK -> calculateRespiratoryScore(features);
        };

        RiskLevel riskLevel = resolveRiskLevel(score);

        PredictionItemDto item = new PredictionItemDto();
        item.setPredictionType(predictionType.name());
        item.setRiskScore(round(score));
        item.setConfidence(0.85);
        item.setRiskLevel(riskLevel.name());
        item.setExplanation(fallbackExplanation(predictionType));
        item.setIsMainPrediction(Boolean.FALSE);

        return item;
    }

    private double calculateCardiovascularScore(Map<String, Object> features) {
        double score = 0.02;

        score += interpretationContribution(features, VitalSignType.BLOOD_PRESSURE_SYSTOLIC, 0.03, 0.10, 0.22);
        score += interpretationContribution(features, VitalSignType.BLOOD_PRESSURE_DIASTOLIC, 0.03, 0.08, 0.18);
        score += interpretationContribution(features, VitalSignType.CHOLESTEROL, 0.03, 0.08, 0.18);
        score += interpretationContribution(features, VitalSignType.BMI, 0.02, 0.06, 0.14);
        score += interpretationContribution(features, VitalSignType.HEART_RATE, 0.02, 0.05, 0.10);

        return Math.min(score, 0.95);
    }

    private double calculateDiabetesScore(Map<String, Object> features) {
        double score = 0.02;

        score += interpretationContribution(features, VitalSignType.GLUCOSE, 0.04, 0.12, 0.26);
        score += interpretationContribution(features, VitalSignType.BMI, 0.02, 0.06, 0.14);

        return Math.min(score, 0.92);
    }

    private double calculateGeneralScore(Map<String, Object> features) {
        double score = 0.02;

        score += interpretationContribution(features, VitalSignType.HEART_RATE, 0.03, 0.08, 0.16);
        score += interpretationContribution(features, VitalSignType.BODY_TEMPERATURE, 0.03, 0.08, 0.16);
        score += interpretationContribution(features, VitalSignType.OXYGEN_SATURATION, 0.04, 0.10, 0.22);
        score += interpretationContribution(features, VitalSignType.BLOOD_PRESSURE_SYSTOLIC, 0.02, 0.06, 0.12);
        score += interpretationContribution(features, VitalSignType.GLUCOSE, 0.02, 0.05, 0.10);

        return Math.min(score, 0.90);
    }

    private double calculateSepsisScore(Map<String, Object> features) {
        double score = 0.02;

        int relevantSignals = 0;

        relevantSignals += countIfRelevant(features, VitalSignType.BODY_TEMPERATURE);
        relevantSignals += countIfRelevant(features, VitalSignType.HEART_RATE);
        relevantSignals += countIfRelevant(features, VitalSignType.OXYGEN_SATURATION);
        relevantSignals += countIfRelevant(features, VitalSignType.BLOOD_PRESSURE_SYSTOLIC);
        relevantSignals += countIfRelevant(features, VitalSignType.GLUCOSE);

        if (relevantSignals < 2) {
            return 0.02;
        }

        score += interpretationContribution(features, VitalSignType.BODY_TEMPERATURE, 0.02, 0.10, 0.25);
        score += interpretationContribution(features, VitalSignType.HEART_RATE, 0.02, 0.08, 0.20);
        score += interpretationContribution(features, VitalSignType.OXYGEN_SATURATION, 0.03, 0.10, 0.25);
        score += interpretationContribution(features, VitalSignType.BLOOD_PRESSURE_SYSTOLIC, 0.02, 0.08, 0.20);
        score += interpretationContribution(features, VitalSignType.GLUCOSE, 0.01, 0.05, 0.12);

        return Math.min(score, 0.94);
    }

    private double calculateRespiratoryScore(Map<String, Object> features) {
        double score = 0.02;

        score += interpretationContribution(features, VitalSignType.OXYGEN_SATURATION, 0.04, 0.12, 0.28);
        score += interpretationContribution(features, VitalSignType.HEART_RATE, 0.02, 0.05, 0.10);

        return Math.min(score, 0.92);
    }

    private double interpretationContribution(Map<String, Object> features,
                                              VitalSignType vitalType,
                                              double lowContribution,
                                              double mediumContribution,
                                              double highContribution) {
        String status = interpretationStatus(features, vitalType);

        if (status == null) {
            return 0.0;
        }

        return switch (status) {
            case "LOW" -> lowContribution;
            case "MEDIUM" -> mediumContribution;
            case "HIGH", "CRITICAL" -> highContribution;
            default -> 0.0;
        };
    }

    private int countIfRelevant(Map<String, Object> features, VitalSignType type) {
        String status = interpretationStatus(features, type);

        if (status == null) {
            return 0;
        }

        return switch (status) {
            case "MEDIUM", "HIGH", "CRITICAL" -> 1;
            default -> 0;
        };
    }

    private String interpretationStatus(Map<String, Object> features, VitalSignType vitalType) {
        Object interpretationsObject = features.get("vitalInterpretations");

        if (interpretationsObject instanceof Map<?, ?> interpretations) {
            Object payload = interpretations.get(vitalType.name());

            if (payload instanceof Map<?, ?> map) {
                Object status = map.get("status");
                return status instanceof String string ? string : null;
            }
        }

        return null;
    }

    private RiskLevel resolveRiskLevel(double score) {
        if (score >= 0.30) {
            return RiskLevel.HIGH;
        }

        if (score >= 0.15) {
            return RiskLevel.MEDIUM;
        }

        if (score >= 0.05) {
            return RiskLevel.LOW;
        }

        return RiskLevel.NEUTRAL;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toBigDecimal(Number value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(value.doubleValue())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private double toDouble(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private Double numericValueIfUsable(VitalSigns vitalSigns,
                                        Map<String, Object> interpretations,
                                        VitalSignType vitalType) {
        if (vitalSigns == null || vitalSigns.getValue() == null) {
            return null;
        }

        Object payload = interpretations.get(vitalType.name());

        if (payload instanceof Map<?, ?> map) {
            Object rawStatus = map.get("status");
            String status = rawStatus instanceof String string ? string : null;

            if (VitalClinicalStatus.OUT_OF_RANGE.name().equals(status)
                    || VitalClinicalStatus.INSUFFICIENT_CONTEXT.name().equals(status)) {
                return null;
            }
        }

        return vitalSigns.getValue().doubleValue();
    }

    private String resolveExplanation(String explanation) {
        if (explanation != null && !explanation.isBlank()) {
            return explanation.trim();
        }

        return "Based on age-aware vital interpretation and patient context";
    }

    private String fallbackExplanation(PredictionType predictionType) {
        return switch (predictionType) {
            case CARDIOVASCULAR_RISK -> "Based on blood pressure, cholesterol, BMI and heart rate interpretation";
            case DIABETES_RISK -> "Based on glucose and BMI interpretation";
            case GENERAL_DETERIORATION -> "Based on core vital signs stability";
            case SEPSIS_RISK -> "Based on temperature, heart rate, blood pressure, oxygen saturation and glucose signals";
            case RESPIRATORY_RISK -> "Based on oxygen saturation and heart rate";
        };
    }

    private record PredictionResolution(PredictionItemDto item, String modelVersion) {
    }
}

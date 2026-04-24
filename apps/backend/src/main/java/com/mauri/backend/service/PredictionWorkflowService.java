package com.mauri.backend.service;

import com.mauri.backend.dto.prediction.PredictionDto;
import com.mauri.backend.dto.prediction.PredictionItemDto;
import com.mauri.backend.dto.prediction.PredictionRequestDto;
import com.mauri.backend.dto.prediction.PredictionResponseDto;
import com.mauri.backend.entity.ConsultNote;
import com.mauri.backend.entity.ConsultNoteVersion;
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
            PredictionDto predictionDto = new PredictionDto();
            predictionDto.setPredictionType(predictionType.name());
            predictionDto.setRiskLevel(normalizeRiskLevel(item.getRiskLevel()));
            predictionDto.setRiskScore(toBigDecimal(item.getRiskScore()));
            predictionDto.setConfidence(toBigDecimal(item.getConfidence()));
            predictionDto.setExplanation(resolveExplanation(item.getExplanation(), resolution.modelVersion()));
            predictionDto.setMainPrediction(index == 0);
            predictionDto.setModelVersion(resolution.modelVersion());
            savedPredictions.add(predictionService.savePrediction(patientId, predictionDto, triggeredByReferenceId));
        }

        return savedPredictions;
    }

    private PredictionRequestDto buildRequest(Patient patient, String triggerSource) {
        PredictionRequestDto request = new PredictionRequestDto();
        request.setPatientId(patient.getId());
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
            latestByType.putIfAbsent(vitalSigns.getType(), vitalSigns);
            VitalInterpretationResult interpretation = vitalInterpretationService.interpret(vitalSigns, recentVitals);
            vitalInterpretations.put(vitalSigns.getType().name(), interpretationPayload(vitalSigns, interpretation));
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
        features.put("growthInterpretationStatus", firstAvailableStatus(vitalInterpretations, VitalSignType.BMI, VitalSignType.WEIGHT, VitalSignType.HEIGHT));
        features.put("growthInterpretationMessage", firstAvailableMessage(vitalInterpretations, VitalSignType.BMI, VitalSignType.WEIGHT, VitalSignType.HEIGHT));

        List<PatientMedication> activeMedications = patientMedicationRepository.findByPatientAndStatusInOrderByStartDateDesc(
                patient,
                List.of(MedicationStatus.ACTIVE)
        );
        features.put("activeMedicationCount", activeMedications.size());
        features.put(
                "activeMedications",
                activeMedications.stream()
                        .limit(10)
                        .map(medication -> medication.getMedicationCatalog() != null
                                ? medication.getMedicationCatalog().getDutchName()
                                : "unknown")
                        .toList()
        );

        List<ConsultNote> recentNotes = consultNoteRepository.findTop5ByPatientOrderByCreatedAtDesc(patient);
        features.put(
                "recentAssessments",
                recentNotes.stream()
                        .map(ConsultNote::getCurrentVersion)
                        .filter(version -> version != null && version.getAssessment() != null && !version.getAssessment().isBlank())
                        .map(ConsultNoteVersion::getAssessment)
                        .toList()
        );
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
        } catch (IllegalStateException exception) {
            log.warn("Using backend fallback predictions for patient {} because the AI service is unavailable.",
                    request.getPatientId());
            return Map.of();
        } catch (Exception exception) {
            log.warn("Falling back to backend prediction heuristics for patient {} after an unexpected AI request failure.",
                    request.getPatientId(),
                    exception);
            return Map.of();
        }

        if (response == null || response.getPredictions() == null || response.getPredictions().isEmpty()) {
            log.warn("AI service returned an empty prediction payload for patient {}. Using backend fallback.", request.getPatientId());
            return Map.of();
        }

        Map<PredictionType, PredictionItemDto> aiPredictions = new LinkedHashMap<>();
        for (PredictionItemDto item : response.getPredictions()) {
            if (!isUsableAiItem(item)) {
                log.warn("Ignoring invalid AI prediction item for patient {} because required fields are missing.", request.getPatientId());
                continue;
            }

            PredictionType predictionType;
            try {
                predictionType = PredictionType.valueOf(item.getPredictionType().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                log.warn("Ignoring AI prediction item with unsupported type '{}' for patient {}.",
                        item.getPredictionType(),
                        request.getPatientId());
                continue;
            }

            try {
                RiskLevel.valueOf(normalizeRiskLevel(item.getRiskLevel()));
            } catch (IllegalArgumentException exception) {
                log.warn("Ignoring AI prediction item with unsupported risk level '{}' for patient {}.",
                        item.getRiskLevel(),
                        request.getPatientId());
                continue;
            }

            aiPredictions.put(predictionType, item);
        }

        return aiPredictions;
    }

    private boolean isUsableAiItem(PredictionItemDto item) {
        return item != null
                && item.getPredictionType() != null
                && !item.getPredictionType().isBlank()
                && item.getRiskLevel() != null
                && !item.getRiskLevel().isBlank()
                && item.getRiskScore() != null
                && item.getConfidence() != null;
    }

    private Map<String, Object> interpretationPayload(VitalSigns vitalSigns, VitalInterpretationResult interpretation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", interpretation.status().name());
        payload.put("message", interpretation.message());
        payload.put("ageGroup", interpretation.ageGroup().name());
        payload.put("contextComplete", interpretation.contextComplete());
        payload.put("measuredAt", vitalSigns.getMeasuredAt() != null ? vitalSigns.getMeasuredAt().toString() : null);
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

    private String firstAvailableStatus(Map<String, Object> interpretations, VitalSignType... vitalTypes) {
        for (VitalSignType vitalType : vitalTypes) {
            String status = interpretationStatusFromPayload(interpretations, vitalType);
            if (status != null) {
                return status;
            }
        }
        return null;
    }

    private String firstAvailableMessage(Map<String, Object> interpretations, VitalSignType... vitalTypes) {
        for (VitalSignType vitalType : vitalTypes) {
            String message = interpretationMessageFromPayload(interpretations, vitalType);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    private List<PredictionType> requestedPredictionTypes(PredictionRequestDto request) {
        if (request.getPredictionTypes() == null || request.getPredictionTypes().isEmpty()) {
            return List.of();
        }
        return request.getPredictionTypes().stream()
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

        double confidence = adjustConfidence(features, score);

        PredictionItemDto item = new PredictionItemDto();
        item.setPredictionType(predictionType.name());
        item.setRiskScore(round(score));
        item.setConfidence(round(confidence));
        item.setRiskLevel(resolveRiskLevel(score).name());
        item.setExplanation(fallbackExplanation(predictionType));
        item.setIsMainPrediction(Boolean.FALSE);
        return item;
    }

    private double calculateCardiovascularScore(Map<String, Object> features) {
        double score = 0.12;
        score += numeric(features.get("age")) >= 65 ? 0.15 : 0.04;
        score += interpretationContribution(features, VitalSignType.BLOOD_PRESSURE_SYSTOLIC, 0.10, 0.20);
        score += interpretationContribution(features, VitalSignType.BLOOD_PRESSURE_DIASTOLIC, 0.08, 0.16);
        score += interpretationContribution(features, VitalSignType.CHOLESTEROL, 0.08, 0.16);

        if (adultBmiApplicable(features)) {
            score += interpretationContribution(features, VitalSignType.BMI, 0.05, 0.12);
        }

        return Math.min(score, 0.95);
    }

    private double calculateDiabetesScore(Map<String, Object> features) {
        double score = 0.10;
        score += interpretationContribution(features, VitalSignType.GLUCOSE, 0.16, 0.30);
        if (adultBmiApplicable(features)) {
            score += interpretationContribution(features, VitalSignType.BMI, 0.08, 0.18);
        }
        score += numeric(features.get("age")) >= 55 ? 0.08 : 0.0;
        return Math.min(score, 0.92);
    }

    private double calculateGeneralScore(Map<String, Object> features) {
        double score = 0.10;
        score += interpretationContribution(features, VitalSignType.HEART_RATE, 0.10, 0.18);
        score += interpretationContribution(features, VitalSignType.BODY_TEMPERATURE, 0.08, 0.16);
        score += interpretationContribution(features, VitalSignType.OXYGEN_SATURATION, 0.12, 0.24);
        score += numeric(features.get("recentConsultCount")) >= 3 ? 0.10 : 0.0;
        return Math.min(score, 0.90);
    }

    private double calculateSepsisScore(Map<String, Object> features) {
        double score = 0.08;
        score += interpretationContribution(features, VitalSignType.BODY_TEMPERATURE, 0.14, 0.28);
        score += interpretationContribution(features, VitalSignType.HEART_RATE, 0.12, 0.24);
        score += numeric(features.get("recentConsultCount")) >= 2 ? 0.08 : 0.0;
        return Math.min(score, 0.94);
    }

    private double calculateRespiratoryScore(Map<String, Object> features) {
        double score = 0.10;
        score += interpretationContribution(features, VitalSignType.OXYGEN_SATURATION, 0.16, 0.32);
        score += interpretationContribution(features, VitalSignType.HEART_RATE, 0.06, 0.12);
        return Math.min(score, 0.92);
    }

    private double adjustConfidence(Map<String, Object> features, double score) {
        double confidence = Math.max(0.55, 0.92 - (score / 2));
        confidence -= insufficientContextCount(features) * 0.04;
        confidence -= outOfRangeCount(features) * 0.06;
        return Math.max(0.35, confidence);
    }

    private int insufficientContextCount(Map<String, Object> features) {
        int count = 0;
        for (VitalSignType vitalType : VitalSignType.values()) {
            String status = interpretationStatus(features, vitalType);
            if (VitalClinicalStatus.INSUFFICIENT_CONTEXT.name().equals(status) || VitalClinicalStatus.NOT_APPLICABLE.name().equals(status)) {
                count++;
            }
        }
        return count;
    }

    private int outOfRangeCount(Map<String, Object> features) {
        int count = 0;
        for (VitalSignType vitalType : VitalSignType.values()) {
            if (VitalClinicalStatus.OUT_OF_RANGE.name().equals(interpretationStatus(features, vitalType))) {
                count++;
            }
        }
        return count;
    }

    private boolean adultBmiApplicable(Map<String, Object> features) {
        return numeric(features.get("age")) >= 20
                && !VitalClinicalStatus.NOT_APPLICABLE.name().equals(interpretationStatus(features, VitalSignType.BMI))
                && !VitalClinicalStatus.INSUFFICIENT_CONTEXT.name().equals(interpretationStatus(features, VitalSignType.BMI));
    }

    private double interpretationContribution(Map<String, Object> features,
                                              VitalSignType vitalType,
                                              double mediumContribution,
                                              double highContribution) {
        String status = interpretationStatus(features, vitalType);
        if (status == null) {
            return 0.0;
        }
        return switch (status) {
            case "MEDIUM" -> mediumContribution;
            case "HIGH", "CRITICAL" -> highContribution;
            default -> 0.0;
        };
    }

    private String interpretationStatus(Map<String, Object> features, VitalSignType vitalType) {
        Object interpretationsObject = features.get("vitalInterpretations");
        if (interpretationsObject instanceof Map<?, ?> interpretations) {
            return interpretationStatusFromWildcard(interpretations, vitalType);
        }
        return null;
    }

    private String interpretationStatusFromPayload(Map<String, Object> interpretations, VitalSignType vitalType) {
        if (interpretations == null || vitalType == null) {
            return null;
        }
        Object payload = interpretations.get(vitalType.name());
        if (payload instanceof Map<?, ?> map) {
            Object status = map.get("status");
            return status instanceof String string ? string : null;
        }
        return null;
    }

    private String interpretationStatusFromWildcard(Map<?, ?> interpretations, VitalSignType vitalType) {
        if (interpretations == null || vitalType == null) {
            return null;
        }
        Object payload = interpretations.get(vitalType.name());
        if (payload instanceof Map<?, ?> map) {
            Object status = map.get("status");
            return status instanceof String string ? string : null;
        }
        return null;
    }

    private String interpretationMessageFromPayload(Map<String, Object> interpretations, VitalSignType vitalType) {
        if (interpretations == null || vitalType == null) {
            return null;
        }
        Object payload = interpretations.get(vitalType.name());
        if (payload instanceof Map<?, ?> map) {
            Object message = map.get("message");
            return message instanceof String string ? string : null;
        }
        return null;
    }

    private RiskLevel resolveRiskLevel(double score) {
        if (score >= 0.80) {
            return RiskLevel.CRITICAL;
        }
        if (score >= 0.60) {
            return RiskLevel.HIGH;
        }
        if (score >= 0.30) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private double numeric(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private Double numericValue(VitalSigns vitalSigns) {
        if (vitalSigns == null || vitalSigns.getValue() == null) {
            return null;
        }
        return vitalSigns.getValue().doubleValue();
    }

    private Double numericValueIfUsable(VitalSigns vitalSigns,
                                        Map<String, Object> interpretations,
                                        VitalSignType vitalType) {
        String interpretationStatus = interpretationStatusFromPayload(interpretations, vitalType);
        if (VitalClinicalStatus.OUT_OF_RANGE.name().equals(interpretationStatus)
                || VitalClinicalStatus.INSUFFICIENT_CONTEXT.name().equals(interpretationStatus)
                || VitalClinicalStatus.NOT_APPLICABLE.name().equals(interpretationStatus)) {
            return null;
        }
        return numericValue(vitalSigns);
    }

    private String normalizeRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return RiskLevel.LOW.name();
        }
        return riskLevel.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveExplanation(String explanation, String modelVersion) {
        if (explanation != null && !explanation.isBlank()) {
            return explanation.trim();
        }
        if ("backend-fallback".equals(modelVersion)) {
            return "Fallback backend prediction based on age-aware vital interpretation and latest patient context";
        }
        return "AI prediction based on the latest patient context";
    }

    private String fallbackExplanation(PredictionType predictionType) {
        return switch (predictionType) {
            case CARDIOVASCULAR_RISK ->
                    "Fallback backend prediction based on blood pressure, cholesterol and age-aware interpretation";
            case DIABETES_RISK ->
                    "Fallback backend prediction based on glucose, BMI and age-aware interpretation";
            case GENERAL_DETERIORATION ->
                    "Fallback backend prediction based on heart rate, temperature, oxygen saturation and recent consult activity";
            case SEPSIS_RISK ->
                    "Fallback backend prediction based on temperature, heart rate and recent clinical activity";
            case RESPIRATORY_RISK ->
                    "Fallback backend prediction based on oxygen saturation and heart rate";
        };
    }

    private record PredictionResolution(PredictionItemDto item, String modelVersion) {
    }
}

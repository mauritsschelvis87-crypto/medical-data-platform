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
import com.mauri.backend.enums.MedicationStatus;
import com.mauri.backend.enums.PredictionType;
import com.mauri.backend.enums.RiskLevel;
import com.mauri.backend.repository.ConsultNoteRepository;
import com.mauri.backend.repository.PatientMedicationRepository;
import com.mauri.backend.repository.VitalSignsRepository;
import com.mauri.backend.service.ai.AiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PredictionWorkflowService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PatientService patientService;
    private final VitalSignsRepository vitalSignsRepository;
    private final PatientMedicationRepository patientMedicationRepository;
    private final ConsultNoteRepository consultNoteRepository;
    private final PredictionService predictionService;
    private final AiService aiService;

    public PredictionWorkflowService(PatientService patientService,
                                     VitalSignsRepository vitalSignsRepository,
                                     PatientMedicationRepository patientMedicationRepository,
                                     ConsultNoteRepository consultNoteRepository,
                                     PredictionService predictionService,
                                     AiService aiService) {
        this.patientService = patientService;
        this.vitalSignsRepository = vitalSignsRepository;
        this.patientMedicationRepository = patientMedicationRepository;
        this.consultNoteRepository = consultNoteRepository;
        this.predictionService = predictionService;
        this.aiService = aiService;
    }

    @Transactional
    public List<PredictionDto> recalculatePredictions(Long patientId, String triggerSource, Long triggeredByReferenceId) {
        Patient patient = patientService.getPatientEntityById(patientId);
        PredictionRequestDto request = buildRequest(patient, triggerSource);

        PredictionResponseDto response;
        try {
            response = aiService.calculatePredictions(request);
        } catch (Exception ex) {
            response = buildFallbackResponse(request);
        }

        List<PredictionDto> savedPredictions = new ArrayList<>();
        if (response == null || response.getPredictions() == null || response.getPredictions().isEmpty()) {
            return savedPredictions;
        }

        for (PredictionItemDto item : response.getPredictions()) {
            PredictionDto predictionDto = new PredictionDto();
            predictionDto.setPredictionType(item.getPredictionType());
            predictionDto.setRiskLevel(item.getRiskLevel());
            predictionDto.setRiskScore(toBigDecimal(item.getRiskScore()));
            predictionDto.setConfidence(toBigDecimal(item.getConfidence()));
            predictionDto.setExplanation(item.getExplanation());
            predictionDto.setMainPrediction(Boolean.TRUE.equals(item.getIsMainPrediction()));
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
        features.put("patientNumber", patient.getPatientNumber());
        features.put("birthDate", patient.getBirthDate() != null ? patient.getBirthDate().format(DATE_FORMATTER) : null);
        features.put("age", patient.getBirthDate() != null ? Period.between(patient.getBirthDate(), LocalDate.now()).getYears() : null);
        features.put("gender", patient.getGender() != null ? patient.getGender().name() : null);

        List<VitalSigns> recentVitals = vitalSignsRepository.findTop10ByPatientOrderByMeasuredAtDesc(patient);
        if (!recentVitals.isEmpty()) {
            VitalSigns latest = recentVitals.get(0);
            features.put("bloodPressureSystolic", latest.getBloodPressureSystolic());
            features.put("bloodPressureDiastolic", latest.getBloodPressureDiastolic());
            features.put("heartRate", latest.getHeartRate());
            features.put("temperature", latest.getTemperature());
            features.put("glucose", latest.getGlucose());
            features.put("bmi", latest.getBmi());
            features.put("weight", latest.getWeight());
            features.put("oxygenSaturation", latest.getOxygenSaturation());
            features.put("cholesterol", latest.getCholesterol());
            features.put("measuredAt", latest.getMeasuredAt() != null ? latest.getMeasuredAt().toString() : null);
        }

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

    private PredictionResponseDto buildFallbackResponse(PredictionRequestDto request) {
        PredictionResponseDto response = new PredictionResponseDto();
        response.setPatientId(request.getPatientId());
        response.setGeneratedAt(java.time.LocalDateTime.now().toString());
        response.setPredictions(generateFallbackPredictions(request.getFeatures()));
        return response;
    }

    private List<PredictionItemDto> generateFallbackPredictions(Map<String, Object> features) {
        List<PredictionItemDto> predictions = new ArrayList<>();
        predictions.add(buildFallbackPrediction(PredictionType.CARDIOVASCULAR_RISK, true, features));
        predictions.add(buildFallbackPrediction(PredictionType.DIABETES_RISK, false, features));
        predictions.add(buildFallbackPrediction(PredictionType.GENERAL_DETERIORATION, false, features));
        return predictions;
    }

    private PredictionItemDto buildFallbackPrediction(PredictionType predictionType,
                                                      boolean mainPrediction,
                                                      Map<String, Object> features) {
        double score = switch (predictionType) {
            case CARDIOVASCULAR_RISK -> calculateCardiovascularScore(features);
            case DIABETES_RISK -> calculateDiabetesScore(features);
            case GENERAL_DETERIORATION -> calculateGeneralScore(features);
            case SEPSIS_RISK -> 0.18;
            case RESPIRATORY_RISK -> 0.22;
        };

        PredictionItemDto item = new PredictionItemDto();
        item.setPredictionType(predictionType.name());
        item.setRiskScore(round(score));
        item.setConfidence(round(Math.max(0.55, 0.92 - (score / 2))));
        item.setRiskLevel(resolveRiskLevel(score).name());
        item.setExplanation("Fallback backend prediction based on latest patient context");
        item.setIsMainPrediction(mainPrediction);
        return item;
    }

    private double calculateCardiovascularScore(Map<String, Object> features) {
        double score = 0.15;
        score += numeric(features.get("age")) >= 65 ? 0.20 : 0.05;
        score += numeric(features.get("bloodPressureSystolic")) >= 140 ? 0.20 : 0.0;
        score += numeric(features.get("cholesterol")) >= 5.5 ? 0.15 : 0.0;
        score += numeric(features.get("bmi")) >= 30 ? 0.10 : 0.0;
        return Math.min(score, 0.95);
    }

    private double calculateDiabetesScore(Map<String, Object> features) {
        double score = 0.12;
        score += numeric(features.get("glucose")) >= 7.0 ? 0.30 : 0.0;
        score += numeric(features.get("bmi")) >= 30 ? 0.20 : 0.0;
        score += numeric(features.get("age")) >= 55 ? 0.10 : 0.0;
        return Math.min(score, 0.92);
    }

    private double calculateGeneralScore(Map<String, Object> features) {
        double score = 0.10;
        score += numeric(features.get("heartRate")) >= 110 ? 0.18 : 0.0;
        score += numeric(features.get("temperature")) >= 38.5 ? 0.16 : 0.0;
        score += numeric(features.get("oxygenSaturation")) <= 92 && numeric(features.get("oxygenSaturation")) > 0 ? 0.24 : 0.0;
        score += numeric(features.get("recentConsultCount")) >= 3 ? 0.10 : 0.0;
        return Math.min(score, 0.90);
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
}

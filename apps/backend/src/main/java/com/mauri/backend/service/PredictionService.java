package com.mauri.backend.service;

import com.mauri.backend.dto.prediction.PredictionDto;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.Prediction;
import com.mauri.backend.enums.PredictionType;
import com.mauri.backend.enums.RiskLevel;
import com.mauri.backend.mapper.PredictionMapper;
import com.mauri.backend.repository.PredictionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final PatientService patientService;
    private final PredictionMapper predictionMapper;
    private final TimelineService timelineService;

    public PredictionService(PredictionRepository predictionRepository,
                             PatientService patientService,
                             PredictionMapper predictionMapper,
                             TimelineService timelineService) {
        this.predictionRepository = predictionRepository;
        this.patientService = patientService;
        this.predictionMapper = predictionMapper;
        this.timelineService = timelineService;
    }

    public List<PredictionDto> getPredictionsForPatient(UUID patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return predictionRepository.findByPatientOrderByPredictionTimestampDesc(patient)
                .stream()
                .map(predictionMapper::toDto)
                .toList();
    }

    public List<PredictionDto> getPredictionsForPatientByType(UUID patientId, PredictionType predictionType) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return predictionRepository.findByPatientAndPredictionTypeOrderByPredictionTimestampDesc(patient, predictionType)
                .stream()
                .map(predictionMapper::toDto)
                .toList();
    }

    public List<PredictionDto> getMainPredictionsForPatient(UUID patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return predictionRepository.findByPatientAndMainPredictionTrueOrderByPredictionTimestampDesc(patient)
                .stream()
                .map(predictionMapper::toDto)
                .toList();
    }

    public List<PredictionDto> getLatestPredictionsForPatient(UUID patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return predictionRepository.findLatestPredictionsPerType(patient)
                .stream()
                .map(predictionMapper::toDto)
                .toList();
    }

    @Transactional
    public PredictionDto savePrediction(UUID patientId, PredictionDto predictionDto) {
        return savePrediction(patientId, predictionDto, null);
    }

    @Transactional
    public PredictionDto savePrediction(UUID patientId, PredictionDto predictionDto, UUID triggeredByReferenceId) {
        Patient patient = patientService.getPatientEntityById(patientId);
        Prediction prediction = predictionMapper.toEntity(predictionDto);
        validatePrediction(predictionDto, prediction);
        prediction.setPatient(patient);
        prediction.setPredictionTimestamp(LocalDateTime.now());
        prediction.setTriggeredByReferenceId(triggeredByReferenceId);

        // 1. Zoek de vorige voorspelling van hetzelfde type om risico te vergelijken
        Optional<Prediction> previousPrediction = predictionRepository
                .findFirstByPatientAndPredictionTypeOrderByPredictionTimestampDesc(patient, prediction.getPredictionType());

        if (previousPrediction.isPresent()) {
            RiskLevel prevLevel = previousPrediction.get().getRiskLevel();
            RiskLevel currentLevel = prediction.getRiskLevel();

            prediction.setPreviousRiskLevel(prevLevel);

            if (currentLevel.ordinal() > prevLevel.ordinal()) {
                prediction.setRiskIncreased(true);
            }
        }

        prediction.setRequiresConfirmation(false);
        prediction.setConfirmed(false);
        prediction.setConfirmedAt(null);
        prediction.setConfirmedBy(null);
        prediction.setThresholdTriggered(false);

        Prediction savedPrediction = predictionRepository.save(prediction);

        String summary = String.format("Nieuwe %s voorspelling: %s risico",
                prediction.getPredictionType(), prediction.getRiskLevel());
        if (prediction.isRiskIncreased()) {
            summary += " (risico gestegen)";
        }
        timelineService.createEvent(
                patient,
                com.mauri.backend.enums.TimelineEventType.PREDICTION_GENERATED,
                savedPrediction.getId(),
                "Prediction",
                "Prediction generated",
                summary,
                savedPrediction.getPredictionTimestamp()
        );

        return predictionMapper.toDto(savedPrediction);
    }

    private void validatePrediction(PredictionDto predictionDto, Prediction prediction) {
        if (prediction == null) {
            throw new IllegalArgumentException("Prediction payload is required.");
        }
        if (prediction.getPredictionType() == null) {
            throw new IllegalArgumentException("Prediction type is required.");
        }
        if (prediction.getRiskLevel() == null) {
            throw new IllegalArgumentException("Risk level is required.");
        }
        if (prediction.getRiskScore() == null) {
            throw new IllegalArgumentException("Risk score is required.");
        }
        if (prediction.getConfidence() == null) {
            throw new IllegalArgumentException("Prediction confidence is required.");
        }
        if (prediction.getRiskScore().signum() < 0 || prediction.getConfidence().signum() < 0) {
            throw new IllegalArgumentException("Risk score and confidence must be zero or greater.");
        }
        if (prediction.getRiskScore().compareTo(java.math.BigDecimal.valueOf(100)) > 0
                || prediction.getConfidence().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Risk score and confidence must not exceed 100.");
        }
        if (predictionDto != null) {
            predictionDto.setPredictionType(prediction.getPredictionType().name().toUpperCase(Locale.ROOT));
            predictionDto.setRiskLevel(prediction.getRiskLevel().name().toUpperCase(Locale.ROOT));
        }
    }
}

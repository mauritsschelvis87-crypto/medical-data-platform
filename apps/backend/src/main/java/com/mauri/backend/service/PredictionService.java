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
            
            // Vergelijk ordinals: LOW (0), MEDIUM (1), HIGH (2), CRITICAL (3)
            if (currentLevel.ordinal() > prevLevel.ordinal()) {
                prediction.setRiskIncreased(true);
            }
        }

        // 2. Bepaal of bevestiging van de arts nodig is (Warning System)
        // Bevestiging is nodig als het risico is gestegen OF als het risico HIGH/CRITICAL is
        if (prediction.isRiskIncreased() || 
            prediction.getRiskLevel() == RiskLevel.HIGH || 
            prediction.getRiskLevel() == RiskLevel.CRITICAL) {
            prediction.setRequiresConfirmation(true);
            prediction.setThresholdTriggered(true);
        }

        Prediction savedPrediction = predictionRepository.save(prediction);

        // 3. Update de tijdlijn
        String summary = String.format("Nieuwe %s voorspelling: %s risico", 
                prediction.getPredictionType(), prediction.getRiskLevel());
        if (prediction.isRiskIncreased()) {
            summary += " (RISICO GESTEGEN)";
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

    @Transactional
    public PredictionDto confirmPrediction(UUID predictionId, String doctorName) {
        Prediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Voorspelling niet gevonden met id: " + predictionId));

        prediction.setConfirmed(true);
        prediction.setConfirmedAt(LocalDateTime.now());
        prediction.setConfirmedBy(doctorName);
        prediction.setRequiresConfirmation(false);

        Prediction updatedPrediction = predictionRepository.save(prediction);
        
        // Optioneel: Tijdlijn event voor bevestiging
        timelineService.createEvent(
                prediction.getPatient(),
                com.mauri.backend.enums.TimelineEventType.PREDICTION_GENERATED,
                updatedPrediction.getId(),
                "Prediction",
                "Prediction confirmed",
                "Risico bevestigd door arts: " + doctorName,
                updatedPrediction.getConfirmedAt()
        );

        return predictionMapper.toDto(updatedPrediction);
    }
}

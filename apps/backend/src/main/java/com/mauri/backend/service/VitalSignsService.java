package com.mauri.backend.service;

import com.mauri.backend.dto.vitals.CreateVitalSignsRequest;
import com.mauri.backend.dto.vitals.VitalSignsDto;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.TimelineEventType;
import com.mauri.backend.enums.VitalSignSource;
import com.mauri.backend.enums.VitalSignType;
import com.mauri.backend.mapper.VitalSignsMapper;
import com.mauri.backend.repository.VitalSignsRepository;
import com.mauri.backend.service.interpretation.GrowthInterpreterService;
import com.mauri.backend.service.interpretation.VitalMeasurementValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class VitalSignsService {

    private static final Logger log = LoggerFactory.getLogger(VitalSignsService.class);

    private final VitalSignsRepository vitalSignsRepository;
    private final PatientService patientService;
    private final VitalSignsMapper vitalSignsMapper;
    private final TimelineService timelineService;
    private final PredictionWorkflowService predictionWorkflowService;
    private final GrowthInterpreterService growthInterpreterService;
    private final VitalMeasurementValidationService vitalMeasurementValidationService;

    public VitalSignsService(VitalSignsRepository vitalSignsRepository,
                             PatientService patientService,
                             VitalSignsMapper vitalSignsMapper,
                             TimelineService timelineService,
                             PredictionWorkflowService predictionWorkflowService,
                             GrowthInterpreterService growthInterpreterService,
                             VitalMeasurementValidationService vitalMeasurementValidationService) {
        this.vitalSignsRepository = vitalSignsRepository;
        this.patientService = patientService;
        this.vitalSignsMapper = vitalSignsMapper;
        this.timelineService = timelineService;
        this.predictionWorkflowService = predictionWorkflowService;
        this.growthInterpreterService = growthInterpreterService;
        this.vitalMeasurementValidationService = vitalMeasurementValidationService;
    }

    @Transactional(readOnly = true)
    public List<VitalSignsDto> getVitalSignsForPatient(UUID patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);
        List<VitalSigns> vitalSigns = vitalSignsRepository.findByPatientOrderByMeasuredAtDesc(patient);
        return vitalSigns.stream().map(vitalSign -> vitalSignsMapper.toDto(vitalSign, vitalSigns)).toList();
    }

    @Transactional(readOnly = true)
    public List<VitalSignsDto> getLatestVitalSignsForPatient(UUID patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);
        List<VitalSigns> vitalSigns = vitalSignsRepository.findLatestPerTypeByPatient(patient);
        return vitalSigns.stream().map(vitalSign -> vitalSignsMapper.toDto(vitalSign, vitalSigns)).toList();
    }

    @Transactional
    public VitalSignsDto createVitalSigns(UUID patientId, CreateVitalSignsRequest request) {
        Patient patient = patientService.getPatientEntityById(patientId);

        VitalSigns vitalSigns = new VitalSigns();
        vitalSigns.setPatient(patient);
        VitalSignType type = VitalSignType.valueOf(request.getType().trim().toUpperCase(Locale.ROOT));
        vitalSigns.setType(type);
        vitalSigns.setValue(request.getValue());
        vitalSigns.setUnit(request.getUnit().trim());
        vitalSigns.setMeasuredAt(request.getMeasuredAt() != null ? request.getMeasuredAt() : LocalDateTime.now());
        vitalSigns.setSource(resolveSource(request.getSource()));
        vitalSigns.setSourceObservationCode(request.getSourceObservationCode());
        vitalSigns.setSourceDescription(request.getSourceDescription());
        vitalMeasurementValidationService.validateForPersistence(vitalSigns);
        validateDerivedBmiIfPossible(patient, vitalSigns);

        VitalSigns savedVitalSigns = vitalSignsRepository.save(vitalSigns);
        recalculateBmiIfNeeded(patient, savedVitalSigns);

        timelineService.createEvent(
                patient,
                TimelineEventType.VITAL_SIGNS_RECORDED,
                savedVitalSigns.getId(),
                "VitalSigns",
                "Vital signs recorded",
                savedVitalSigns.getType() + ": " + savedVitalSigns.getValue() + " " + savedVitalSigns.getUnit(),
                savedVitalSigns.getMeasuredAt()
        );
        predictionWorkflowService.recalculatePredictions(patientId, "VITAL_SIGNS_RECORDED", savedVitalSigns.getId());

        List<VitalSigns> contextVitals = vitalSignsRepository.findLatestPerTypeByPatient(patient);
        return vitalSignsMapper.toDto(savedVitalSigns, contextVitals);
    }

    private void recalculateBmiIfNeeded(Patient patient, VitalSigns savedVitalSigns) {
        if (savedVitalSigns.getType() != VitalSignType.WEIGHT && savedVitalSigns.getType() != VitalSignType.HEIGHT) {
            return;
        }

        VitalSigns latestWeight = savedVitalSigns.getType() == VitalSignType.WEIGHT
                ? savedVitalSigns
                : vitalSignsRepository.findFirstByPatientAndTypeOrderByMeasuredAtDesc(patient, VitalSignType.WEIGHT);
        VitalSigns latestHeight = savedVitalSigns.getType() == VitalSignType.HEIGHT
                ? savedVitalSigns
                : vitalSignsRepository.findFirstByPatientAndTypeOrderByMeasuredAtDesc(patient, VitalSignType.HEIGHT);

        if (latestWeight == null || latestHeight == null) {
            return;
        }

        if (!growthInterpreterService.shouldCalculateBmi(patient, latestWeight, latestHeight)) {
            return;
        }

        BigDecimal bmi = growthInterpreterService.calculateBmi(latestWeight.getValue(), latestHeight.getValue());
        if (bmi == null) {
            return;
        }

        VitalSigns bmiVital = new VitalSigns();
        bmiVital.setPatient(patient);
        bmiVital.setType(VitalSignType.BMI);
        bmiVital.setValue(bmi);
        bmiVital.setUnit("kg/m2");
        bmiVital.setMeasuredAt(latestWeight.getMeasuredAt().isBefore(latestHeight.getMeasuredAt())
                ? latestWeight.getMeasuredAt()
                : latestHeight.getMeasuredAt());
        bmiVital.setSource(VitalSignSource.MANUAL);
        bmiVital.setSourceDescription("Calculated from latest weight and height");
        try {
            vitalMeasurementValidationService.validateForPersistence(bmiVital);
            vitalSignsRepository.save(bmiVital);
        } catch (IllegalArgumentException exception) {
            log.warn("Skipping derived BMI because the calculated value is implausible: {}",
                    exception.getMessage());
        }
    }

    private void validateDerivedBmiIfPossible(Patient patient, VitalSigns candidateVitalSigns) {
        if (candidateVitalSigns.getType() != VitalSignType.WEIGHT && candidateVitalSigns.getType() != VitalSignType.HEIGHT) {
            return;
        }

        VitalSigns weight = candidateVitalSigns.getType() == VitalSignType.WEIGHT
                ? candidateVitalSigns
                : vitalSignsRepository.findFirstByPatientAndTypeOrderByMeasuredAtDesc(patient, VitalSignType.WEIGHT);
        VitalSigns height = candidateVitalSigns.getType() == VitalSignType.HEIGHT
                ? candidateVitalSigns
                : vitalSignsRepository.findFirstByPatientAndTypeOrderByMeasuredAtDesc(patient, VitalSignType.HEIGHT);

        if (weight == null || height == null || !growthInterpreterService.shouldCalculateBmi(patient, weight, height)) {
            return;
        }

        BigDecimal bmi = growthInterpreterService.calculateBmi(weight.getValue(), height.getValue());
        if (bmi == null) {
            return;
        }

        VitalSigns bmiVital = new VitalSigns();
        bmiVital.setPatient(patient);
        bmiVital.setType(VitalSignType.BMI);
        bmiVital.setValue(bmi);
        bmiVital.setUnit("kg/m2");
        bmiVital.setMeasuredAt(candidateVitalSigns.getMeasuredAt());
        bmiVital.setSource(candidateVitalSigns.getSource());
        bmiVital.setSourceDescription("Calculated from pending weight and height validation");
        vitalMeasurementValidationService.validateForPersistence(bmiVital);
    }

    private VitalSignSource resolveSource(String source) {
        if (source == null || source.isBlank()) {
            return VitalSignSource.MANUAL;
        }
        return VitalSignSource.valueOf(source.trim().toUpperCase(Locale.ROOT));
    }
}

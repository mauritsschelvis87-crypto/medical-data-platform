package com.mauri.backend.service;

import com.mauri.backend.dto.medication.CreatePatientMedicationRequest;
import com.mauri.backend.dto.medication.PatientMedicationDto;
import com.mauri.backend.dto.medication.UpdatePatientMedicationRequest;
import com.mauri.backend.entity.MedicationCatalog;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.PatientMedication;
import com.mauri.backend.enums.MedicationStatus;
import com.mauri.backend.enums.TimelineEventType;
import com.mauri.backend.exception.ResourceNotFoundException;
import com.mauri.backend.mapper.MedicationMapper;
import com.mauri.backend.repository.MedicationCatalogRepository;
import com.mauri.backend.repository.PatientMedicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientMedicationService {

    private final PatientMedicationRepository patientMedicationRepository;
    private final MedicationCatalogRepository medicationCatalogRepository;
    private final PatientService patientService;
    private final MedicationMapper medicationMapper;
    private final TimelineService timelineService;
    private final PredictionWorkflowService predictionWorkflowService;

    public PatientMedicationService(PatientMedicationRepository patientMedicationRepository,
                                    MedicationCatalogRepository medicationCatalogRepository,
                                    PatientService patientService,
                                    MedicationMapper medicationMapper,
                                    TimelineService timelineService,
                                    PredictionWorkflowService predictionWorkflowService) {
        this.patientMedicationRepository = patientMedicationRepository;
        this.medicationCatalogRepository = medicationCatalogRepository;
        this.patientService = patientService;
        this.medicationMapper = medicationMapper;
        this.timelineService = timelineService;
        this.predictionWorkflowService = predictionWorkflowService;
    }

    public List<PatientMedicationDto> getMedicationsForPatient(Long patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return patientMedicationRepository.findByPatientOrderByStartDateDesc(patient)
                .stream()
                .map(medicationMapper::toPatientMedicationDto)
                .toList();
    }

    public List<PatientMedicationDto> getMedicationsForPatientByStatus(Long patientId, MedicationStatus status) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return patientMedicationRepository.findByPatientAndStatusOrderByStartDateDesc(patient, status)
                .stream()
                .map(medicationMapper::toPatientMedicationDto)
                .toList();
    }

    @Transactional
    public PatientMedicationDto createPatientMedication(Long patientId, CreatePatientMedicationRequest request) {
        Patient patient = patientService.getPatientEntityById(patientId);

        MedicationCatalog medicationCatalog = medicationCatalogRepository.findById(request.getMedicationCatalogId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Medication catalog entry not found with id: " + request.getMedicationCatalogId()
                ));

        PatientMedication patientMedication = new PatientMedication();
        patientMedication.setPatient(patient);
        patientMedication.setMedicationCatalog(medicationCatalog);
        patientMedication.setDosage(request.getDosage());
        patientMedication.setFrequency(request.getFrequency());
        patientMedication.setStartDate(request.getStartDate());
        patientMedication.setEndDate(request.getEndDate());
        patientMedication.setReason(request.getReason());
        patientMedication.setPrescribedBy(request.getPrescribedBy());
        patientMedication.setStatus(MedicationStatus.ACTIVE);

        PatientMedication savedPatientMedication = patientMedicationRepository.save(patientMedication);

        timelineService.createEvent(
                patient,
                TimelineEventType.MEDICATION_STARTED,
                savedPatientMedication.getId(),
                "PatientMedication",
                "Medication started",
                buildMedicationDescription(savedPatientMedication),
                savedPatientMedication.getCreatedAt()
        );
        predictionWorkflowService.recalculatePredictions(patientId, "MEDICATION_CREATED", savedPatientMedication.getId());

        return medicationMapper.toPatientMedicationDto(savedPatientMedication);
    }

    @Transactional
    public PatientMedicationDto updatePatientMedication(Long patientId,
                                                        Long patientMedicationId,
                                                        UpdatePatientMedicationRequest request) {
        Patient patient = patientService.getPatientEntityById(patientId);
        PatientMedication patientMedication = patientMedicationRepository.findById(patientMedicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient medication not found with id: " + patientMedicationId
                ));

        if (!patientMedication.getPatient().getId().equals(patient.getId())) {
            throw new ResourceNotFoundException("Patient medication does not belong to patient: " + patientId);
        }

        if (request.getDosage() != null) {
            patientMedication.setDosage(request.getDosage());
        }
        if (request.getFrequency() != null) {
            patientMedication.setFrequency(request.getFrequency());
        }
        if (request.getStartDate() != null) {
            patientMedication.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            patientMedication.setEndDate(request.getEndDate());
        }
        if (request.getReason() != null) {
            patientMedication.setReason(request.getReason());
        }
        if (request.getPrescribedBy() != null) {
            patientMedication.setPrescribedBy(request.getPrescribedBy());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            patientMedication.setStatus(MedicationStatus.valueOf(request.getStatus().trim().toUpperCase()));
        }

        PatientMedication savedPatientMedication = patientMedicationRepository.save(patientMedication);
        TimelineEventType timelineEventType = savedPatientMedication.getStatus() == MedicationStatus.STOPPED
                ? TimelineEventType.MEDICATION_STOPPED
                : TimelineEventType.MEDICATION_CHANGED;

        timelineService.createEvent(
                patient,
                timelineEventType,
                savedPatientMedication.getId(),
                "PatientMedication",
                timelineEventType == TimelineEventType.MEDICATION_STOPPED ? "Medication stopped" : "Medication updated",
                buildMedicationDescription(savedPatientMedication),
                savedPatientMedication.getUpdatedAt()
        );
        predictionWorkflowService.recalculatePredictions(patientId, "MEDICATION_UPDATED", savedPatientMedication.getId());
        return medicationMapper.toPatientMedicationDto(savedPatientMedication);
    }

    private String buildMedicationDescription(PatientMedication patientMedication) {
        String medicationName = null;

        if (patientMedication.getMedicationCatalog() != null) {
            medicationName = patientMedication.getMedicationCatalog().getDutchName();

            if (medicationName == null || medicationName.isBlank()) {
                medicationName = patientMedication.getMedicationCatalog().getLatinName();
            }
        }

        if (medicationName == null || medicationName.isBlank()) {
            medicationName = "Unknown medication";
        }

        return medicationName + " - " + patientMedication.getDosage() + " - " + patientMedication.getFrequency();
    }
}

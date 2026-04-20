package com.mauri.backend.service;

import com.mauri.backend.dto.medication.CreatePatientMedicationRequest;
import com.mauri.backend.dto.medication.PatientMedicationDto;
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

import java.util.List;

@Service
public class PatientMedicationService {

    private final PatientMedicationRepository patientMedicationRepository;
    private final MedicationCatalogRepository medicationCatalogRepository;
    private final PatientService patientService;
    private final MedicationMapper medicationMapper;
    private final TimelineService timelineService;

    public PatientMedicationService(PatientMedicationRepository patientMedicationRepository,
                                    MedicationCatalogRepository medicationCatalogRepository,
                                    PatientService patientService,
                                    MedicationMapper medicationMapper,
                                    TimelineService timelineService) {
        this.patientMedicationRepository = patientMedicationRepository;
        this.medicationCatalogRepository = medicationCatalogRepository;
        this.patientService = patientService;
        this.medicationMapper = medicationMapper;
        this.timelineService = timelineService;
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
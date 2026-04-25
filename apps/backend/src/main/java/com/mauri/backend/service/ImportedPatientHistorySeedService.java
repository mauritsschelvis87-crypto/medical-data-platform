package com.mauri.backend.service;

import com.mauri.backend.entity.ConsultNote;
import com.mauri.backend.entity.ConsultNoteVersion;
import com.mauri.backend.entity.MedicationCatalog;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.PatientMedication;
import com.mauri.backend.enums.ConsultNoteStatus;
import com.mauri.backend.enums.MedicationStatus;
import com.mauri.backend.enums.TimelineEventType;
import com.mauri.backend.mapper.MedicationMapper;
import com.mauri.backend.repository.ConsultNoteRepository;
import com.mauri.backend.repository.ConsultNoteVersionRepository;
import com.mauri.backend.repository.MedicationCatalogRepository;
import com.mauri.backend.repository.PatientMedicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ImportedPatientHistorySeedService {

    private static final List<String> DOCTORS = List.of(
            "Dr. Jeckyll Hyde",
            "Dr. Marieke van Dijk",
            "Dr. Sophie de Boer",
            "Dr. Lars Meijer"
    );

    private static final List<NoteTemplate> NOTE_TEMPLATES = List.of(
            new NoteTemplate(
                    "Patient reported good symptom control with occasional morning stiffness.",
                    "Vitals remained stable without fever or respiratory distress.",
                    "Chronic condition stable with mild intermittent complaints.",
                    "Continue current monitoring and repeat evaluation during the next scheduled review."
            ),
            new NoteTemplate(
                    "Patient described a brief flare-up after increased daily activity.",
                    "Physical examination showed no acute deterioration and no new neurological deficits.",
                    "Transient exacerbation resolved conservatively without red flags.",
                    "Document conservative management, reinforce self-care advice and review medication tolerance."
            )
    );

    private static final List<String> ACTIVE_MEDICATION_REASONS = List.of(
            "Maintenance treatment for long-term symptom control.",
            "Ongoing therapy to support cardiovascular risk reduction.",
            "Preventive treatment after previous recurrent complaints."
    );

    private static final List<String> HISTORICAL_MEDICATION_REASONS = List.of(
            "Completed course after a previous symptomatic flare-up.",
            "Temporary treatment used during an earlier recovery period.",
            "Previous medication regimen discontinued after clinical improvement."
    );

    private static final List<String> ACTIVE_FREQUENCIES = List.of(
            "1 time a day",
            "2 times a day",
            "Every evening"
    );

    private static final List<String> HISTORICAL_FREQUENCIES = List.of(
            "2 times a day",
            "3 times a day",
            "As needed"
    );

    private final ConsultNoteRepository consultNoteRepository;
    private final ConsultNoteVersionRepository consultNoteVersionRepository;
    private final PatientMedicationRepository patientMedicationRepository;
    private final MedicationCatalogRepository medicationCatalogRepository;
    private final MedicationCatalogImportService medicationCatalogImportService;
    private final MedicationMapper medicationMapper;
    private final TimelineService timelineService;
    private final EntityTimestampBackfillService entityTimestampBackfillService;

    public ImportedPatientHistorySeedService(ConsultNoteRepository consultNoteRepository,
                                            ConsultNoteVersionRepository consultNoteVersionRepository,
                                            PatientMedicationRepository patientMedicationRepository,
                                            MedicationCatalogRepository medicationCatalogRepository,
                                            MedicationCatalogImportService medicationCatalogImportService,
                                            MedicationMapper medicationMapper,
                                            TimelineService timelineService,
                                            EntityTimestampBackfillService entityTimestampBackfillService) {
        this.consultNoteRepository = consultNoteRepository;
        this.consultNoteVersionRepository = consultNoteVersionRepository;
        this.patientMedicationRepository = patientMedicationRepository;
        this.medicationCatalogRepository = medicationCatalogRepository;
        this.medicationCatalogImportService = medicationCatalogImportService;
        this.medicationMapper = medicationMapper;
        this.timelineService = timelineService;
        this.entityTimestampBackfillService = entityTimestampBackfillService;
    }

    @Transactional
    public SeedResult seedHistory(List<Patient> importedPatients, boolean replaceExistingData) {
        if (importedPatients == null || importedPatients.isEmpty()) {
            return new SeedResult(0, 0);
        }

        medicationCatalogImportService.ensureCatalogLoaded();
        List<MedicationCatalog> activeCatalog = medicationCatalogRepository.findByActiveTrueOrderByDutchNameAsc();
        if (activeCatalog.isEmpty()) {
            return seedNotesOnly(importedPatients, replaceExistingData);
        }

        int consultNotesSeeded = 0;
        int medicationsSeeded = 0;

        for (int index = 0; index < importedPatients.size(); index++) {
            Patient patient = importedPatients.get(index);
            if (!replaceExistingData && patientAlreadyHasSeededHistory(patient)) {
                continue;
            }

            consultNotesSeeded += seedConsultNotes(patient, index);
            medicationsSeeded += seedMedications(patient, index, activeCatalog);
        }

        return new SeedResult(consultNotesSeeded, medicationsSeeded);
    }

    private SeedResult seedNotesOnly(List<Patient> importedPatients, boolean replaceExistingData) {
        int consultNotesSeeded = 0;

        for (int index = 0; index < importedPatients.size(); index++) {
            Patient patient = importedPatients.get(index);
            if (!replaceExistingData && patientAlreadyHasSeededHistory(patient)) {
                continue;
            }
            consultNotesSeeded += seedConsultNotes(patient, index);
        }

        return new SeedResult(consultNotesSeeded, 0);
    }

    private boolean patientAlreadyHasSeededHistory(Patient patient) {
        return !consultNoteRepository.findTop5ByPatientOrderByCreatedAtDesc(patient).isEmpty()
                || !patientMedicationRepository.findByPatientOrderByStartDateDesc(patient).isEmpty();
    }

    private int seedConsultNotes(Patient patient, int patientIndex) {
        int seeded = 0;
        for (int templateIndex = 0; templateIndex < NOTE_TEMPLATES.size(); templateIndex++) {
            NoteTemplate template = NOTE_TEMPLATES.get((patientIndex + templateIndex) % NOTE_TEMPLATES.size());
            LocalDateTime timestamp = consultationTimestamp(patientIndex, templateIndex);
            String doctor = doctor(patientIndex + templateIndex);

            ConsultNote consultNote = new ConsultNote();
            consultNote.setPatient(patient);
            consultNote.setCreatedBy(doctor);
            consultNote.setStatus(ConsultNoteStatus.FINALIZED);
            consultNote.setCreatedAt(timestamp);
            consultNote.setUpdatedAt(timestamp);
            consultNote = consultNoteRepository.save(consultNote);
            consultNoteRepository.flush();
            entityTimestampBackfillService.backfillConsultNoteTimestamps(consultNote.getId(), timestamp, timestamp);

            ConsultNoteVersion version = new ConsultNoteVersion();
            version.setConsultNote(consultNote);
            version.setVersionNumber("1.0");
            version.setSubjective(template.subjective());
            version.setObjective(template.objective());
            version.setAssessment(template.assessment());
            version.setPlan(template.plan());
            version.setCreatedBy(doctor);
            version.setCreatedAt(timestamp);
            version.setUpdatedAt(timestamp);
            version = consultNoteVersionRepository.save(version);
            consultNoteVersionRepository.flush();
            entityTimestampBackfillService.backfillConsultNoteVersionTimestamps(version.getId(), timestamp, timestamp);

            consultNote.setCurrentVersion(version);
            consultNote.getVersions().add(version);
            consultNote.setUpdatedAt(timestamp);
            consultNote = consultNoteRepository.save(consultNote);
            consultNoteRepository.flush();
            entityTimestampBackfillService.backfillConsultNoteTimestamps(consultNote.getId(), timestamp, timestamp);

            timelineService.createEvent(
                    patient,
                    TimelineEventType.CONSULT_NOTE_CREATED,
                    consultNote.getId(),
                    "ConsultNote",
                    "Consult note created",
                    "Assessment: " + template.assessment(),
                    timestamp
            );
            seeded++;
        }

        return seeded;
    }

    private int seedMedications(Patient patient, int patientIndex, List<MedicationCatalog> activeCatalog) {
        MedicationCatalog historicalCatalog = activeCatalog.get(patientIndex % activeCatalog.size());
        MedicationCatalog activeMedicationCatalog = activeCatalog.get((patientIndex + 7) % activeCatalog.size());

        seedHistoricalMedication(patient, patientIndex, historicalCatalog);
        seedActiveMedication(patient, patientIndex, activeMedicationCatalog);
        return 2;
    }

    private void seedHistoricalMedication(Patient patient, int patientIndex, MedicationCatalog medicationCatalog) {
        LocalDate startDate = LocalDate.now().minusMonths(10L + (patientIndex % 4)).minusDays(patientIndex % 12);
        LocalDate endDate = startDate.plusMonths(2L + (patientIndex % 3));
        LocalDateTime startedAt = LocalDateTime.of(startDate, LocalTime.of(9, 10));
        LocalDateTime stoppedAt = LocalDateTime.of(endDate, LocalTime.of(10, 5));

        PatientMedication medication = new PatientMedication();
        medication.setPatient(patient);
        medication.setMedicationCatalog(medicationCatalog);
        medication.setDosage(resolveDosage(medicationCatalog, patientIndex));
        medication.setFrequency(HISTORICAL_FREQUENCIES.get(patientIndex % HISTORICAL_FREQUENCIES.size()));
        medication.setStartDate(startDate);
        medication.setEndDate(endDate);
        medication.setReason(HISTORICAL_MEDICATION_REASONS.get(patientIndex % HISTORICAL_MEDICATION_REASONS.size()));
        medication.setPrescribedBy(doctor(patientIndex));
        medication.setStatus(MedicationStatus.STOPPED);
        medication.setCreatedAt(startedAt);
        medication.setUpdatedAt(stoppedAt);
        medication = patientMedicationRepository.save(medication);
        patientMedicationRepository.flush();
        entityTimestampBackfillService.backfillPatientMedicationTimestamps(medication.getId(), startedAt, stoppedAt);

        timelineService.createEvent(
                patient,
                TimelineEventType.MEDICATION_STARTED,
                medication.getId(),
                "PatientMedication",
                "Medication started",
                describeMedication(medication),
                startedAt
        );
        timelineService.createEvent(
                patient,
                TimelineEventType.MEDICATION_STOPPED,
                medication.getId(),
                "PatientMedication",
                "Medication stopped",
                describeMedication(medication),
                stoppedAt
        );
    }

    private void seedActiveMedication(Patient patient, int patientIndex, MedicationCatalog medicationCatalog) {
        LocalDate startDate = LocalDate.now().minusMonths(1L + (patientIndex % 3)).minusDays((patientIndex % 9) + 2L);
        LocalDateTime startedAt = LocalDateTime.of(startDate, LocalTime.of(8, 45));

        PatientMedication medication = new PatientMedication();
        medication.setPatient(patient);
        medication.setMedicationCatalog(medicationCatalog);
        medication.setDosage(resolveDosage(medicationCatalog, patientIndex + 3));
        medication.setFrequency(ACTIVE_FREQUENCIES.get(patientIndex % ACTIVE_FREQUENCIES.size()));
        medication.setStartDate(startDate);
        medication.setEndDate(null);
        medication.setReason(ACTIVE_MEDICATION_REASONS.get(patientIndex % ACTIVE_MEDICATION_REASONS.size()));
        medication.setPrescribedBy(doctor(patientIndex + 1));
        medication.setStatus(MedicationStatus.ACTIVE);
        medication.setCreatedAt(startedAt);
        medication.setUpdatedAt(startedAt);
        medication = patientMedicationRepository.save(medication);
        patientMedicationRepository.flush();
        entityTimestampBackfillService.backfillPatientMedicationTimestamps(medication.getId(), startedAt, startedAt);

        timelineService.createEvent(
                patient,
                TimelineEventType.MEDICATION_STARTED,
                medication.getId(),
                "PatientMedication",
                "Medication started",
                describeMedication(medication),
                startedAt
        );
    }

    private LocalDateTime consultationTimestamp(int patientIndex, int templateIndex) {
        long monthsAgo = templateIndex == 0 ? 7L + (patientIndex % 4) : 2L + (patientIndex % 3);
        long daysAgo = (patientIndex % 9L) + (templateIndex * 4L);
        return LocalDateTime.of(
                LocalDate.now().minusMonths(monthsAgo).minusDays(daysAgo),
                LocalTime.of(11 - templateIndex, 20)
        );
    }

    private String resolveDosage(MedicationCatalog medicationCatalog, int index) {
        String defaultDosage = medicationCatalog.getDefaultDosage();
        if (defaultDosage != null && !defaultDosage.isBlank()) {
            return defaultDosage.trim();
        }

        return switch (index % 4) {
            case 0 -> "5 mg";
            case 1 -> "10 mg";
            case 2 -> "20 mg";
            default -> "1 tablet";
        };
    }

    private String doctor(int index) {
        return DOCTORS.get(index % DOCTORS.size());
    }

    private String describeMedication(PatientMedication medication) {
        return "%s - %s - %s".formatted(
                medicationMapper.extractCatalogName(medication.getMedicationCatalog()),
                medication.getDosage(),
                medication.getFrequency()
        );
    }

    public record SeedResult(int consultNotesSeeded, int medicationsSeeded) {
    }

    private record NoteTemplate(String subjective, String objective, String assessment, String plan) {
    }
}

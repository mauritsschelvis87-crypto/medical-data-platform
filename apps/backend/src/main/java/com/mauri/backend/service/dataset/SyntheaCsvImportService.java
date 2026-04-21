package com.mauri.backend.service.dataset;

import com.mauri.backend.dto.dataset.CreateDatasetImportRequest;
import com.mauri.backend.entity.ConsultNote;
import com.mauri.backend.entity.ConsultNoteVersion;
import com.mauri.backend.entity.DatasetImport;
import com.mauri.backend.entity.MedicationCatalog;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.PatientMedication;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.ConsultNoteStatus;
import com.mauri.backend.enums.DatasetImportStatus;
import com.mauri.backend.enums.Gender;
import com.mauri.backend.enums.MedicationStatus;
import com.mauri.backend.enums.TimelineEventType;
import com.mauri.backend.enums.VitalSignSource;
import com.mauri.backend.exception.ResourceNotFoundException;
import com.mauri.backend.repository.ConsultNoteRepository;
import com.mauri.backend.repository.ConsultNoteVersionRepository;
import com.mauri.backend.repository.DatasetImportRepository;
import com.mauri.backend.repository.MedicationCatalogRepository;
import com.mauri.backend.repository.PatientMedicationRepository;
import com.mauri.backend.repository.PatientRepository;
import com.mauri.backend.repository.PredictionRepository;
import com.mauri.backend.repository.TimelineEventRepository;
import com.mauri.backend.repository.VitalSignsRepository;
import com.mauri.backend.service.PredictionWorkflowService;
import com.mauri.backend.service.TimelineService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class SyntheaCsvImportService {

    private static final String DEMO_DOCTOR = "Dr. Jonathan Hyde";

    private final DatasetImportRepository datasetImportRepository;
    private final PatientRepository patientRepository;
    private final VitalSignsRepository vitalSignsRepository;
    private final MedicationCatalogRepository medicationCatalogRepository;
    private final PatientMedicationRepository patientMedicationRepository;
    private final ConsultNoteRepository consultNoteRepository;
    private final ConsultNoteVersionRepository consultNoteVersionRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final PredictionRepository predictionRepository;
    private final TimelineService timelineService;
    private final PredictionWorkflowService predictionWorkflowService;

    public SyntheaCsvImportService(DatasetImportRepository datasetImportRepository,
                                   PatientRepository patientRepository,
                                   VitalSignsRepository vitalSignsRepository,
                                   MedicationCatalogRepository medicationCatalogRepository,
                                   PatientMedicationRepository patientMedicationRepository,
                                   ConsultNoteRepository consultNoteRepository,
                                   ConsultNoteVersionRepository consultNoteVersionRepository,
                                   TimelineEventRepository timelineEventRepository,
                                   PredictionRepository predictionRepository,
                                   TimelineService timelineService,
                                   PredictionWorkflowService predictionWorkflowService) {
        this.datasetImportRepository = datasetImportRepository;
        this.patientRepository = patientRepository;
        this.vitalSignsRepository = vitalSignsRepository;
        this.medicationCatalogRepository = medicationCatalogRepository;
        this.patientMedicationRepository = patientMedicationRepository;
        this.consultNoteRepository = consultNoteRepository;
        this.consultNoteVersionRepository = consultNoteVersionRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.predictionRepository = predictionRepository;
        this.timelineService = timelineService;
        this.predictionWorkflowService = predictionWorkflowService;
    }

    @Transactional
    public DatasetImport importDataset(CreateDatasetImportRequest request) {
        Path sourceDirectory = resolveSourceDirectory(request.getSourcePath());
        validateRequiredFiles(sourceDirectory);

        DatasetImport datasetImport = new DatasetImport();
        datasetImport.setDatasetName(defaultIfBlank(request.getDatasetName(), "Synthea CSV"));
        datasetImport.setSourceFileName(sourceDirectory.toString());
        datasetImport.setImportStatus(DatasetImportStatus.RUNNING);
        datasetImport.setStartedAt(LocalDateTime.now());
        datasetImport.setVersionTag(defaultIfBlank(request.getNormalizationVersion(), "synthea-normalized-v1"));
        datasetImport.setChecksum(request.getChecksum());
        datasetImport = datasetImportRepository.save(datasetImport);

        try {
            ImportContext context = new ImportContext(sourceDirectory, request);
            parseMedicationCatalog(context);
            parseEncounters(context);
            parseObservations(context);
            parseMedications(context);
            int importedPatients = parsePatients(context);

            context.importedPatientIds().forEach(patientId ->
                    predictionWorkflowService.recalculatePredictions(patientId, "DATASET_IMPORT", null)
            );

            datasetImport.setRecordsReceived(context.patientRowsSeen);
            datasetImport.setRecordsProcessed(importedPatients);
            datasetImport.setRecordsFailed(context.failedRows);
            datasetImport.setImportStatus(context.failedRows > 0 ? DatasetImportStatus.PARTIAL_SUCCESS : DatasetImportStatus.COMPLETED);
            datasetImport.setFinishedAt(LocalDateTime.now());
            return datasetImportRepository.save(datasetImport);
        } catch (Exception ex) {
            datasetImport.setImportStatus(DatasetImportStatus.FAILED);
            datasetImport.setFinishedAt(LocalDateTime.now());
            datasetImport.setErrorMessage(ex.getMessage());
            return datasetImportRepository.save(datasetImport);
        }
    }

    private void parseMedicationCatalog(ImportContext context) throws IOException {
        for (CSVRecord record : parseCsv(context.sourceDirectory.resolve("medications.csv"))) {
            String code = read(record, "CODE");
            String description = read(record, "DESCRIPTION");
            if (isBlank(code) || isBlank(description)) {
                continue;
            }

            context.catalogByCode.computeIfAbsent(code, ignored -> upsertMedicationCatalog(code, description));
        }
    }

    private void parseEncounters(ImportContext context) throws IOException {
        for (CSVRecord record : parseCsv(context.sourceDirectory.resolve("encounters.csv"))) {
            String patientId = read(record, "PATIENT");
            if (isBlank(patientId)) {
                continue;
            }

            EncounterSummary encounter = new EncounterSummary();
            encounter.start = parseDateTime(read(record, "START"));
            encounter.stop = parseDateTime(read(record, "STOP"));
            encounter.encounterClass = defaultIfBlank(read(record, "ENCOUNTERCLASS"), "ambulatory");
            encounter.description = defaultIfBlank(read(record, "DESCRIPTION"), "Imported encounter");
            encounter.reasonDescription = defaultIfBlank(read(record, "REASONDESCRIPTION"), "Clinical follow-up");
            context.encountersByPatient.computeIfAbsent(patientId, ignored -> new ArrayList<>()).add(encounter);
        }
    }

    private void parseObservations(ImportContext context) throws IOException {
        for (CSVRecord record : parseCsv(context.sourceDirectory.resolve("observations.csv"))) {
            String patientId = read(record, "PATIENT");
            if (isBlank(patientId)) {
                continue;
            }

            LocalDateTime measuredAt = parseDateTime(read(record, "DATE"));
            if (measuredAt == null) {
                continue;
            }

            VitalAccumulator accumulator = context.vitalsByPatient
                    .computeIfAbsent(patientId, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(measuredAt, ignored -> new VitalAccumulator(measuredAt));

            String code = read(record, "CODE");
            BigDecimal value = parseDecimal(read(record, "VALUE"));
            if (value == null) {
                continue;
            }

            switch (code) {
                case "8480-6" -> accumulator.systolic = value.intValue();
                case "8462-4" -> accumulator.diastolic = value.intValue();
                case "8867-4" -> accumulator.heartRate = value.intValue();
                case "8310-5" -> accumulator.temperature = value;
                case "2339-0", "41653-7" -> accumulator.glucose = value;
                case "39156-5" -> accumulator.bmi = value;
                case "29463-7" -> accumulator.weight = value;
                case "2708-6" -> accumulator.oxygenSaturation = value;
                case "2093-3" -> accumulator.cholesterol = value;
                default -> {
                }
            }
        }
    }

    private void parseMedications(ImportContext context) throws IOException {
        for (CSVRecord record : parseCsv(context.sourceDirectory.resolve("medications.csv"))) {
            String patientId = read(record, "PATIENT");
            String code = read(record, "CODE");
            if (isBlank(patientId) || isBlank(code)) {
                continue;
            }

            MedicationCatalog catalog = context.catalogByCode.computeIfAbsent(code, ignored ->
                    upsertMedicationCatalog(code, defaultIfBlank(read(record, "DESCRIPTION"), code))
            );

            ImportedMedication medication = new ImportedMedication();
            medication.catalog = catalog;
            medication.startDate = parseDate(read(record, "START"));
            medication.endDate = parseDate(read(record, "STOP"));
            medication.reason = defaultIfBlank(read(record, "REASONDESCRIPTION"), "Imported from Synthea medication history");
            medication.description = defaultIfBlank(read(record, "DESCRIPTION"), catalog.getDutchName());
            medication.status = medication.endDate == null || medication.endDate.isAfter(LocalDate.now())
                    ? MedicationStatus.ACTIVE
                    : MedicationStatus.COMPLETED;

            context.medicationsByPatient.computeIfAbsent(patientId, ignored -> new ArrayList<>()).add(medication);
        }
    }

    private int parsePatients(ImportContext context) throws IOException {
        int importedCount = 0;

        for (CSVRecord record : parseCsv(context.sourceDirectory.resolve("patients.csv"))) {
            context.patientRowsSeen++;

            String syntheaPatientId = read(record, "ID");
            if (isBlank(syntheaPatientId)) {
                context.failedRows++;
                continue;
            }

            try {
                Patient patient = buildPatient(record, syntheaPatientId);
                if (Boolean.TRUE.equals(context.request.getReplaceExisting())) {
                    replaceExistingPatient(patient.getPatientNumber());
                } else if (patientRepository.existsByPatientNumber(patient.getPatientNumber())) {
                    continue;
                }

                Patient savedPatient = patientRepository.save(patient);
                importedCount++;
                context.importedPatientIds.add(savedPatient.getId());

                importVitals(savedPatient, context.vitalsByPatient.get(syntheaPatientId));
                importMedications(savedPatient, context.medicationsByPatient.get(syntheaPatientId));
                importConsultNotes(savedPatient, context.encountersByPatient.get(syntheaPatientId));
            } catch (Exception ex) {
                context.failedRows++;
            }
        }

        return importedCount;
    }

    private Patient buildPatient(CSVRecord record, String syntheaPatientId) {
        Patient patient = new Patient();
        patient.setPatientNumber(buildPatientNumber(syntheaPatientId));
        patient.setFirstName(defaultIfBlank(read(record, "FIRST"), "Unknown"));
        patient.setLastName(defaultIfBlank(read(record, "LAST"), "Patient"));
        patient.setBirthDate(Optional.ofNullable(parseDate(read(record, "BIRTHDATE"))).orElse(LocalDate.of(1980, 1, 1)));
        patient.setGender(resolveGender(read(record, "GENDER")));
        patient.setPhone(normalizePhone(read(record, "SSN")));
        patient.setEmail(buildEmail(record));
        patient.setAddressLine(read(record, "ADDRESS"));
        patient.setPostalCode(read(record, "ZIP"));
        patient.setCity(read(record, "CITY"));
        patient.setCountry(defaultIfBlank(read(record, "STATE"), "USA"));
        return patient;
    }

    private void importVitals(Patient patient, Map<LocalDateTime, VitalAccumulator> vitalsByTimestamp) {
        if (vitalsByTimestamp == null || vitalsByTimestamp.isEmpty()) {
            return;
        }

        List<VitalAccumulator> accumulators = new ArrayList<>(vitalsByTimestamp.values());
        accumulators.sort(Comparator.comparing(accumulator -> accumulator.measuredAt));

        for (VitalAccumulator accumulator : accumulators) {
            VitalSigns vitalSigns = new VitalSigns();
            vitalSigns.setPatient(patient);
            vitalSigns.setBloodPressureSystolic(accumulator.systolic);
            vitalSigns.setBloodPressureDiastolic(accumulator.diastolic);
            vitalSigns.setHeartRate(accumulator.heartRate);
            vitalSigns.setTemperature(accumulator.temperature);
            vitalSigns.setGlucose(accumulator.glucose);
            vitalSigns.setBmi(accumulator.bmi);
            vitalSigns.setWeight(accumulator.weight);
            vitalSigns.setOxygenSaturation(accumulator.oxygenSaturation);
            vitalSigns.setCholesterol(accumulator.cholesterol);
            vitalSigns.setMeasuredAt(accumulator.measuredAt);
            vitalSigns.setRecordedAt(accumulator.measuredAt);
            vitalSigns.setSource(VitalSignSource.IMPORT);

            VitalSigns saved = vitalSignsRepository.save(vitalSigns);
            timelineService.createEvent(
                    patient,
                    TimelineEventType.VITAL_SIGNS_RECORDED,
                    saved.getId(),
                    "VitalSigns",
                    "Imported vital signs",
                    buildVitalDescription(saved),
                    saved.getMeasuredAt()
            );
        }
    }

    private void importMedications(Patient patient, List<ImportedMedication> importedMedications) {
        if (importedMedications == null || importedMedications.isEmpty()) {
            return;
        }

        for (ImportedMedication importedMedication : importedMedications) {
            PatientMedication patientMedication = new PatientMedication();
            patientMedication.setPatient(patient);
            patientMedication.setMedicationCatalog(importedMedication.catalog);
            patientMedication.setDosage(defaultIfBlank(importedMedication.catalog.getDefaultDosage(), "Standard dose"));
            patientMedication.setFrequency("As prescribed");
            patientMedication.setStartDate(Optional.ofNullable(importedMedication.startDate).orElse(LocalDate.now()));
            patientMedication.setEndDate(importedMedication.endDate);
            patientMedication.setReason(importedMedication.reason);
            patientMedication.setPrescribedBy(DEMO_DOCTOR);
            patientMedication.setStatus(importedMedication.status);

            PatientMedication saved = patientMedicationRepository.save(patientMedication);
            timelineService.createEvent(
                    patient,
                    TimelineEventType.MEDICATION_STARTED,
                    saved.getId(),
                    "PatientMedication",
                    "Imported medication",
                    importedMedication.description,
                    saved.getStartDate().atStartOfDay()
            );
        }
    }

    private void importConsultNotes(Patient patient, List<EncounterSummary> encounters) {
        if (encounters == null || encounters.isEmpty()) {
            return;
        }

        encounters.sort(Comparator.comparing(encounter -> Optional.ofNullable(encounter.start).orElse(LocalDateTime.now())));

        for (EncounterSummary encounter : encounters) {
            ConsultNote consultNote = new ConsultNote();
            consultNote.setPatient(patient);
            consultNote.setCreatedBy(DEMO_DOCTOR);
            consultNote.setStatus(ConsultNoteStatus.FINALIZED);
            consultNote.setCreatedAt(encounter.start);
            consultNote.setUpdatedAt(Optional.ofNullable(encounter.stop).orElse(encounter.start));

            ConsultNote savedConsultNote = consultNoteRepository.save(consultNote);

            ConsultNoteVersion version = new ConsultNoteVersion();
            version.setConsultNote(savedConsultNote);
            version.setVersionNumber("1.0");
            version.setSubjective("Imported Synthea encounter: " + encounter.description);
            version.setObjective("Encounter class: " + encounter.encounterClass);
            version.setAssessment(encounter.reasonDescription);
            version.setPlan("Review imported clinical context and continue routine follow-up.");
            version.setCreatedBy(DEMO_DOCTOR);
            version.setChangeReason("Imported from Synthea CSV dataset");
            version.setCreatedAt(encounter.start);
            version.setUpdatedAt(Optional.ofNullable(encounter.stop).orElse(encounter.start));

            ConsultNoteVersion savedVersion = consultNoteVersionRepository.save(version);
            savedConsultNote.setCurrentVersion(savedVersion);
            savedConsultNote.getVersions().add(savedVersion);
            consultNoteRepository.save(savedConsultNote);

            timelineService.createEvent(
                    patient,
                    TimelineEventType.CONSULT_NOTE_CREATED,
                    savedConsultNote.getId(),
                    "ConsultNote",
                    "Imported consult note",
                    encounter.description,
                    Optional.ofNullable(encounter.start).orElse(LocalDateTime.now())
            );
        }
    }

    private MedicationCatalog upsertMedicationCatalog(String code, String description) {
        return medicationCatalogRepository.findByCode(code)
                .orElseGet(() -> {
                    MedicationCatalog catalog = new MedicationCatalog();
                    catalog.setCode(code);
                    catalog.setDutchName(description);
                    catalog.setLatinName(description);
                    catalog.setDefaultDosage("Standard dose");
                    catalog.setAdvice("Imported from Synthea CSV");
                    catalog.setActive(true);
                    return medicationCatalogRepository.save(catalog);
                });
    }

    private void replaceExistingPatient(String patientNumber) {
        patientRepository.findByPatientNumber(patientNumber).ifPresent(existingPatient -> {
            predictionRepository.deleteByPatient(existingPatient);
            timelineEventRepository.deleteByPatient(existingPatient);
            patientMedicationRepository.deleteByPatient(existingPatient);
            vitalSignsRepository.deleteByPatient(existingPatient);
            consultNoteRepository.deleteByPatient(existingPatient);
            patientRepository.delete(existingPatient);
        });
    }

    private Path resolveSourceDirectory(String sourcePath) {
        if (isBlank(sourcePath)) {
            throw new ResourceNotFoundException("sourcePath is required for Synthea CSV import");
        }

        Path directory = Path.of(sourcePath).toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            throw new ResourceNotFoundException("Synthea CSV directory not found: " + directory);
        }
        return directory;
    }

    private void validateRequiredFiles(Path sourceDirectory) {
        for (String fileName : List.of("patients.csv", "encounters.csv", "observations.csv", "medications.csv")) {
            if (!Files.exists(sourceDirectory.resolve(fileName))) {
                throw new ResourceNotFoundException("Required Synthea CSV file missing: " + fileName);
            }
        }
    }

    private CSVParser parseCsv(Path path) throws IOException {
        return CSVParser.parse(
                Files.newBufferedReader(path, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).build()
        );
    }

    private String read(CSVRecord record, String header) {
        return record.isMapped(header) ? record.get(header).trim() : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private String buildPatientNumber(String syntheaPatientId) {
        String normalized = syntheaPatientId.replace("-", "").toUpperCase(Locale.ROOT);
        return "SYN-" + normalized.substring(0, Math.min(10, normalized.length()));
    }

    private Gender resolveGender(String gender) {
        if (gender == null) {
            return Gender.UNKNOWN;
        }
        return switch (gender.trim().toUpperCase(Locale.ROOT)) {
            case "M", "MALE" -> Gender.MALE;
            case "F", "FEMALE" -> Gender.FEMALE;
            default -> Gender.UNKNOWN;
        };
    }

    private String buildEmail(CSVRecord record) {
        String firstName = defaultIfBlank(read(record, "FIRST"), "patient").toLowerCase(Locale.ROOT);
        String lastName = defaultIfBlank(read(record, "LAST"), "demo").toLowerCase(Locale.ROOT);
        return firstName + "." + lastName + "@demo.med";
    }

    private String normalizePhone(String seed) {
        if (isBlank(seed)) {
            return null;
        }
        String digits = seed.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits.substring(0, Math.min(10, digits.length()));
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            LocalDateTime dateTime = parseDateTime(value);
            return dateTime != null ? dateTime.toLocalDate() : null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        if (value.trim().length() == 10) {
            try {
                return LocalDate.parse(value.trim()).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildVitalDescription(VitalSigns vitalSigns) {
        List<String> parts = new ArrayList<>();
        if (vitalSigns.getBloodPressureSystolic() != null && vitalSigns.getBloodPressureDiastolic() != null) {
            parts.add("Blood pressure " + vitalSigns.getBloodPressureSystolic() + "/" + vitalSigns.getBloodPressureDiastolic());
        }
        if (vitalSigns.getHeartRate() != null) {
            parts.add("Heart rate " + vitalSigns.getHeartRate());
        }
        if (vitalSigns.getWeight() != null) {
            parts.add("Weight " + vitalSigns.getWeight() + " kg");
        }
        return parts.isEmpty() ? "Imported vital signs" : String.join(", ", parts);
    }

    private static final class ImportContext {
        private final Path sourceDirectory;
        private final CreateDatasetImportRequest request;
        private final Map<String, MedicationCatalog> catalogByCode = new HashMap<>();
        private final Map<String, List<EncounterSummary>> encountersByPatient = new HashMap<>();
        private final Map<String, Map<LocalDateTime, VitalAccumulator>> vitalsByPatient = new HashMap<>();
        private final Map<String, List<ImportedMedication>> medicationsByPatient = new HashMap<>();
        private final List<Long> importedPatientIds = new ArrayList<>();
        private int patientRowsSeen = 0;
        private int failedRows = 0;

        private ImportContext(Path sourceDirectory, CreateDatasetImportRequest request) {
            this.sourceDirectory = sourceDirectory;
            this.request = request;
        }

        private List<Long> importedPatientIds() {
            return importedPatientIds;
        }
    }

    private static final class EncounterSummary {
        private LocalDateTime start;
        private LocalDateTime stop;
        private String encounterClass;
        private String description;
        private String reasonDescription;
    }

    private static final class ImportedMedication {
        private MedicationCatalog catalog;
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;
        private String description;
        private MedicationStatus status;
    }

    private static final class VitalAccumulator {
        private final LocalDateTime measuredAt;
        private Integer systolic;
        private Integer diastolic;
        private Integer heartRate;
        private BigDecimal temperature;
        private BigDecimal glucose;
        private BigDecimal bmi;
        private BigDecimal weight;
        private BigDecimal oxygenSaturation;
        private BigDecimal cholesterol;

        private VitalAccumulator(LocalDateTime measuredAt) {
            this.measuredAt = measuredAt;
        }
    }
}

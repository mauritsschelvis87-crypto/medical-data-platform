package com.mauri.backend.service;

import com.mauri.backend.entity.DatasetImport;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.VitalSignSource;
import com.mauri.backend.enums.VitalSignType;
import com.mauri.backend.exception.CsvImportValidationException;
import com.mauri.backend.repository.PatientRepository;
import com.mauri.backend.repository.VitalSignsRepository;
import com.mauri.backend.service.interpretation.VitalMeasurementValidationService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class VitalSignsImportService {

    private static final String FILE_NAME = "vital_signs.csv";
    private static final int BATCH_SIZE = 500;
    private static final int MAX_LOGGED_ROW_ERRORS = 25;
    private static final Logger log = LoggerFactory.getLogger(VitalSignsImportService.class);

    private final PatientRepository patientRepository;
    private final VitalSignsRepository vitalSignsRepository;
    private final VitalMeasurementValidationService vitalMeasurementValidationService;

    public VitalSignsImportService(PatientRepository patientRepository,
                                   VitalSignsRepository vitalSignsRepository,
                                   VitalMeasurementValidationService vitalMeasurementValidationService) {
        this.patientRepository = patientRepository;
        this.vitalSignsRepository = vitalSignsRepository;
        this.vitalMeasurementValidationService = vitalMeasurementValidationService;
    }

    @Transactional
    public ImportResult importVitalSigns(String sourceDirectoryPath, DatasetImport datasetImport, boolean replaceExistingData) {
        Path filePath = resolveFile(sourceDirectoryPath);
        int recordsReceived = 0;
        int recordsProcessed = 0;
        int recordsFailed = 0;
        int skippedRecords = 0;
        Set<String> clearedPatientIds = new HashSet<>();
        Map<String, Patient> patientsBySourceId = loadPatientsBySourceId();
        List<VitalSigns> batch = new ArrayList<>(BATCH_SIZE);
        int loggedRowErrors = 0;

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(true).build().parse(reader)) {
            validateHeaders(parser, "patientId", "type", "value", "unit", "measuredAt", "sourceObservationCode", "sourceDescription");

            for (CSVRecord record : parser) {
                recordsReceived++;
                try {
                    Patient patient = patientsBySourceId.get(required(record, "patientId"));
                    if (patient == null) {
                        recordsFailed++;
                        continue;
                    }

                    if (replaceExistingData && clearedPatientIds.add(patient.getId().toString())) {
                        vitalSignsRepository.deleteByPatientAndSource(patient, VitalSignSource.IMPORT);
                    }

                    VitalSigns vitalSigns = new VitalSigns();
                    vitalSigns.setPatient(patient);
                    vitalSigns.setType(parseType(required(record, "type")));
                    vitalSigns.setValue(parseDecimal(required(record, "value")));
                    vitalSigns.setUnit(required(record, "unit"));
                    vitalSigns.setMeasuredAt(parseDateTime(required(record, "measuredAt")));
                    vitalSigns.setSourceObservationCode(optional(record, "sourceObservationCode"));
                    vitalSigns.setSourceDescription(optional(record, "sourceDescription"));
                    vitalSigns.setSource(VitalSignSource.IMPORT);
                    vitalMeasurementValidationService.validateForPersistence(vitalSigns);
                    batch.add(vitalSigns);

                    if (batch.size() >= BATCH_SIZE) {
                        recordsProcessed += persistBatch(batch);
                    }
                } catch (RuntimeException exception) {
                    recordsFailed++;
                    if (loggedRowErrors < MAX_LOGGED_ROW_ERRORS) {
                        loggedRowErrors++;
                        log.warn("Vital signs import failed at csv row {}: {}", record.getRecordNumber(), exception.getMessage());
                    }
                }
            }

            if (!batch.isEmpty()) {
                recordsProcessed += persistBatch(batch);
            }
        } catch (IOException exception) {
            throw new CsvImportValidationException("Unable to read %s: %s".formatted(FILE_NAME, exception.getMessage()), exception);
        }

        return new ImportResult(recordsReceived, recordsProcessed, recordsFailed, skippedRecords, recordsProcessed);
    }

    private Path resolveFile(String sourceDirectoryPath) {
        Path filePath = Path.of(sourceDirectoryPath).toAbsolutePath().normalize().resolve(FILE_NAME);
        if (!Files.exists(filePath)) {
            throw new CsvImportValidationException("Required import file is missing: " + FILE_NAME);
        }
        return filePath;
    }

    private void validateHeaders(CSVParser parser, String... headers) {
        for (String header : headers) {
            if (!parser.getHeaderMap().containsKey(header)) {
                throw new CsvImportValidationException("Missing required header '%s' in %s".formatted(header, FILE_NAME));
            }
        }
    }

    private String required(CSVRecord record, String header) {
        String value = optional(record, header);
        if (value == null || value.isBlank()) {
            throw new CsvImportValidationException("Missing required value for column '%s' at row %d".formatted(header, record.getRecordNumber()));
        }
        return value;
    }

    private String optional(CSVRecord record, String header) {
        return record.isMapped(header) ? record.get(header).trim() : null;
    }

    private Map<String, Patient> loadPatientsBySourceId() {
        Map<String, Patient> patientsBySourceId = new HashMap<>();
        for (Patient patient : patientRepository.findAll()) {
            if (patient.getSourcePatientId() != null && !patient.getSourcePatientId().isBlank()) {
                patientsBySourceId.put(patient.getSourcePatientId(), patient);
            }
        }
        return patientsBySourceId;
    }

    private int persistBatch(List<VitalSigns> batch) {
        int count = batch.size();
        vitalSignsRepository.saveAll(batch);
        batch.clear();
        return count;
    }

    private VitalSignType parseType(String value) {
        try {
            return VitalSignType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new CsvImportValidationException("Unsupported vital sign type: " + value, exception);
        }
    }

    private BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new CsvImportValidationException("Invalid numeric value: " + value, exception);
        }
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new CsvImportValidationException("Invalid datetime value: " + value, exception);
        }
    }

    public record ImportResult(
            int recordsReceived,
            int recordsProcessed,
            int recordsFailed,
            int skippedRecords,
            int importedCount
    ) {
    }
}

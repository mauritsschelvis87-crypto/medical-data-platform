package com.mauri.backend.service;

import com.mauri.backend.entity.DatasetImport;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.enums.Gender;
import com.mauri.backend.exception.CsvImportValidationException;
import com.mauri.backend.repository.PatientRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PatientImportService {

    private static final String FILE_NAME = "patient.csv";

    private final PatientRepository patientRepository;

    public PatientImportService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional
    public ImportResult importPatients(String sourceDirectoryPath, DatasetImport datasetImport, boolean replaceExistingData) {
        Path filePath = resolveFile(sourceDirectoryPath);
        int recordsReceived = 0;
        int recordsProcessed = 0;
        int recordsFailed = 0;
        int skippedRecords = 0;
        List<Patient> importedPatients = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(true).build().parse(reader)) {
            validateHeaders(parser, "patientNumber", "sourcePatientId", "firstName", "lastName", "fullName",
                    "birthDate", "gender", "deceased", "deathDate", "maritalStatus", "race", "ethnicity");

            for (CSVRecord record : parser) {
                recordsReceived++;
                try {
                    String sourcePatientId = required(record, "sourcePatientId");
                    Patient patient = patientRepository.findBySourcePatientId(sourcePatientId).orElseGet(Patient::new);
                    patient.setPatientNumber(required(record, "patientNumber"));
                    patient.setSourcePatientId(sourcePatientId);
                    patient.setFirstName(required(record, "firstName"));
                    patient.setLastName(required(record, "lastName"));
                    patient.setFullName(required(record, "fullName"));
                    patient.setBirthDate(parseDate(required(record, "birthDate")));
                    patient.setGender(parseGender(required(record, "gender")));
                    patient.setDeceased(Boolean.parseBoolean(optional(record, "deceased")));
                    patient.setDeathDate(parseOptionalDate(optional(record, "deathDate")));
                    patient.setMaritalStatus(optional(record, "maritalStatus"));
                    patient.setRace(optional(record, "race"));
                    patient.setEthnicity(optional(record, "ethnicity"));

                    if (!replaceExistingData && patient.getId() != null) {
                        skippedRecords++;
                        continue;
                    }

                    Patient savedPatient = patientRepository.save(patient);
                    importedPatients.add(savedPatient);
                    recordsProcessed++;
                } catch (RuntimeException exception) {
                    recordsFailed++;
                }
            }
        } catch (IOException exception) {
            throw new CsvImportValidationException("Unable to read %s: %s".formatted(FILE_NAME, exception.getMessage()), exception);
        }

        return new ImportResult(recordsReceived, recordsProcessed, recordsFailed, skippedRecords, recordsProcessed, importedPatients);
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

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new CsvImportValidationException("Invalid date value: " + value, exception);
        }
    }

    private LocalDate parseOptionalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseDate(value);
    }

    private Gender parseGender(String value) {
        try {
            return Gender.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new CsvImportValidationException("Unsupported gender value: " + value, exception);
        }
    }

    public record ImportResult(
            int recordsReceived,
            int recordsProcessed,
            int recordsFailed,
            int skippedRecords,
            int importedCount,
            List<Patient> importedPatients
    ) {
    }
}

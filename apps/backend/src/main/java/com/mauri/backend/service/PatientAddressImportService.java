package com.mauri.backend.service;

import com.mauri.backend.entity.DatasetImport;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.PatientAddress;
import com.mauri.backend.exception.CsvImportValidationException;
import com.mauri.backend.repository.PatientAddressRepository;
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

@Service
public class PatientAddressImportService {

    private static final String FILE_NAME = "patient_address.csv";

    private final PatientRepository patientRepository;
    private final PatientAddressRepository patientAddressRepository;

    public PatientAddressImportService(PatientRepository patientRepository,
                                       PatientAddressRepository patientAddressRepository) {
        this.patientRepository = patientRepository;
        this.patientAddressRepository = patientAddressRepository;
    }

    @Transactional
    public ImportResult importAddresses(String sourceDirectoryPath, DatasetImport datasetImport, boolean replaceExistingData) {
        Path filePath = resolveFile(sourceDirectoryPath);
        int recordsReceived = 0;
        int recordsProcessed = 0;
        int recordsFailed = 0;
        int skippedRecords = 0;

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(true).build().parse(reader)) {
            validateHeaders(parser, "patientId", "addressLine", "city", "state", "county", "zipCode");

            for (CSVRecord record : parser) {
                recordsReceived++;
                try {
                    Patient patient = patientRepository.findBySourcePatientId(required(record, "patientId")).orElse(null);
                    if (patient == null) {
                        recordsFailed++;
                        continue;
                    }

                    PatientAddress patientAddress = patientAddressRepository.findByPatient(patient).orElseGet(PatientAddress::new);
                    if (!replaceExistingData && patientAddress.getId() != null) {
                        skippedRecords++;
                        continue;
                    }

                    patientAddress.setPatient(patient);
                    patientAddress.setAddressLine(optional(record, "addressLine"));
                    patientAddress.setCity(optional(record, "city"));
                    patientAddress.setState(optional(record, "state"));
                    patientAddress.setCounty(optional(record, "county"));
                    patientAddress.setZipCode(optional(record, "zipCode"));
                    patientAddressRepository.save(patientAddress);
                    recordsProcessed++;
                } catch (RuntimeException exception) {
                    recordsFailed++;
                }
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

    public record ImportResult(
            int recordsReceived,
            int recordsProcessed,
            int recordsFailed,
            int skippedRecords,
            int importedCount
    ) {
    }
}

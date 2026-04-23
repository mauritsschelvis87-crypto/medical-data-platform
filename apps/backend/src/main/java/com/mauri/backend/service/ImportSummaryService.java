package com.mauri.backend.service;

import com.mauri.backend.exception.CsvImportValidationException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ImportSummaryService {

    private static final String FILE_NAME = "import_summary.csv";

    public Optional<ImportSummary> readSummary(String sourceDirectoryPath) {
        Path filePath = Path.of(sourceDirectoryPath).toAbsolutePath().normalize().resolve(FILE_NAME);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            CSVRecord record = parser.stream().findFirst().orElse(null);
            if (record == null) {
                return Optional.empty();
            }

            Map<String, Integer> headerMap = parser.getHeaderMap();
            return Optional.of(new ImportSummary(
                    readInt(record, headerMap, "recordsReceived", "records_received", "total_records", "total_rows"),
                    readInt(record, headerMap, "recordsProcessed", "records_processed", "processed_records", "processed_rows"),
                    readInt(record, headerMap, "recordsFailed", "records_failed", "failed_records", "failed_rows"),
                    readInt(record, headerMap, "patientCount", "patients", "patient_count"),
                    readInt(record, headerMap, "patientAddressCount", "patient_addresses", "patient_address_count", "address_count"),
                    readInt(record, headerMap, "vitalSignsCount", "vital_signs", "vital_signs_count"),
                    readInt(record, headerMap, "skippedRecords", "skipped_records", "skipped_rows"),
                    readString(record, headerMap, "notes", "summary", "validationSummary", "validation_summary")
            ));
        } catch (IOException exception) {
            throw new CsvImportValidationException("Unable to read %s: %s".formatted(FILE_NAME, exception.getMessage()), exception);
        }
    }

    private Integer readInt(CSVRecord record, Map<String, Integer> headerMap, String... aliases) {
        String value = readString(record, headerMap, aliases);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new CsvImportValidationException("Invalid integer value '%s' in %s".formatted(value, FILE_NAME), exception);
        }
    }

    private String readString(CSVRecord record, Map<String, Integer> headerMap, String... aliases) {
        for (String alias : aliases) {
            for (String header : headerMap.keySet()) {
                if (normalize(header).equals(normalize(alias))) {
                    return record.get(header).trim();
                }
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }

    public record ImportSummary(
            Integer recordsReceived,
            Integer recordsProcessed,
            Integer recordsFailed,
            Integer patientCount,
            Integer patientAddressCount,
            Integer vitalSignsCount,
            Integer skippedRecords,
            String validationSummary
    ) {
    }
}

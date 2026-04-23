package com.mauri.backend.service;

import com.mauri.backend.entity.MedicationCatalog;
import com.mauri.backend.repository.MedicationCatalogRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MedicationCatalogImportService {

    private static final Logger log = LoggerFactory.getLogger(MedicationCatalogImportService.class);
    private static final int CODE_MAX_LENGTH = 50;
    private static final int NAME_MAX_LENGTH = 255;

    private final MedicationCatalogRepository medicationCatalogRepository;
    private final String catalogSourcePath;

    public MedicationCatalogImportService(MedicationCatalogRepository medicationCatalogRepository,
                                          @Value("${app.medication.catalog-source-path:../ai-service/data/raw/medications.csv}")
                                          String catalogSourcePath) {
        this.medicationCatalogRepository = medicationCatalogRepository;
        this.catalogSourcePath = catalogSourcePath;
    }

    @Transactional
    public void ensureCatalogLoaded() {
        if (medicationCatalogRepository.countByActiveTrue() > 0) {
            return;
        }

        Path sourcePath = Paths.get(catalogSourcePath).normalize();
        if (!sourcePath.isAbsolute()) {
            sourcePath = Paths.get("").toAbsolutePath().resolve(sourcePath).normalize();
        }

        if (!Files.exists(sourcePath)) {
            log.warn("Medication catalog source file not found at {}", sourcePath);
            return;
        }

        importCatalog(sourcePath);
    }

    private void importCatalog(Path sourcePath) {
        Set<String> existingCodes = medicationCatalogRepository.findAll()
                .stream()
                .map(MedicationCatalog::getCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());

        Map<String, MedicationCatalog> entriesToCreate = new LinkedHashMap<>();

        try (Reader reader = Files.newBufferedReader(sourcePath);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                String code = read(record, "CODE");
                String description = read(record, "DESCRIPTION");

                if (code == null || description == null) {
                    continue;
                }

                String normalizedCode = truncate(code.trim(), CODE_MAX_LENGTH);
                if (normalizedCode.isBlank() || existingCodes.contains(normalizedCode) || entriesToCreate.containsKey(normalizedCode)) {
                    continue;
                }

                MedicationCatalog medicationCatalog = new MedicationCatalog();
                medicationCatalog.setCode(normalizedCode);
                medicationCatalog.setDutchName(truncate(cleanName(description), NAME_MAX_LENGTH));
                medicationCatalog.setLatinName(null);
                medicationCatalog.setDefaultDosage(null);
                medicationCatalog.setAdvice(null);
                medicationCatalog.setActive(true);
                entriesToCreate.put(normalizedCode, medicationCatalog);
            }
        } catch (IOException exception) {
            log.error("Failed to load medication catalog from {}", sourcePath, exception);
            return;
        }

        if (entriesToCreate.isEmpty()) {
            log.info("Medication catalog import found no new entries at {}", sourcePath);
            return;
        }

        medicationCatalogRepository.saveAll(entriesToCreate.values());
        log.info("Loaded {} medication catalog entries from {}", entriesToCreate.size(), sourcePath);
    }

    private String read(CSVRecord record, String header) {
        if (!record.isMapped(header)) {
            return null;
        }

        String value = record.get(header);
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String cleanName(String rawName) {
        String normalizedWhitespace = rawName.trim().replaceAll("\\s+", " ");
        String withoutBracesPadding = normalizedWhitespace
                .replace("{ ", "{")
                .replace(" }", "}");
        return withoutBracesPadding.toLowerCase(Locale.ROOT).equals(withoutBracesPadding)
                ? withoutBracesPadding
                : normalizedWhitespace;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

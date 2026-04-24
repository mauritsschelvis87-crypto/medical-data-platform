package com.mauri.backend.service;

import com.mauri.backend.dto.medication.MedicationCatalogSearchResultDto;
import com.mauri.backend.dto.medication.MedicationCatalogSelectionDto;
import com.mauri.backend.entity.MedicationCatalog;
import com.mauri.backend.exception.ResourceNotFoundException;
import com.mauri.backend.mapper.MedicationMapper;
import com.mauri.backend.repository.MedicationCatalogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MedicationCatalogService {

    private static final int SEARCH_RESULT_LIMIT = 20;

    private final MedicationCatalogRepository medicationCatalogRepository;
    private final MedicationMapper medicationMapper;
    private final MedicationCatalogImportService medicationCatalogImportService;

    public MedicationCatalogService(MedicationCatalogRepository medicationCatalogRepository,
                                    MedicationMapper medicationMapper,
                                    MedicationCatalogImportService medicationCatalogImportService) {
        this.medicationCatalogRepository = medicationCatalogRepository;
        this.medicationMapper = medicationMapper;
        this.medicationCatalogImportService = medicationCatalogImportService;
    }

    public List<MedicationCatalogSearchResultDto> search(String query) {
        medicationCatalogImportService.ensureCatalogLoaded();

        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        return medicationCatalogRepository.searchActiveCatalog(normalizedQuery, PageRequest.of(0, SEARCH_RESULT_LIMIT))
                .stream()
                .map(medicationMapper::toSearchResultDto)
                .toList();
    }

    public MedicationCatalogSelectionDto getSelection(UUID medicationId) {
        medicationCatalogImportService.ensureCatalogLoaded();

        MedicationCatalog medicationCatalog = medicationCatalogRepository.findById(medicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Medication catalog entry not found with id: " + medicationId));

        return medicationMapper.toSelectionDto(
                medicationCatalog,
                resolveDefaultDosage(medicationCatalog),
                resolveDefaultFrequency(medicationCatalog)
        );
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveDefaultDosage(MedicationCatalog medicationCatalog) {
        String defaultDosage = medicationCatalog.getDefaultDosage();
        if (defaultDosage != null && !defaultDosage.isBlank()) {
            return defaultDosage.trim();
        }
        return "As prescribed";
    }

    private String resolveDefaultFrequency(MedicationCatalog medicationCatalog) {
        String advice = medicationCatalog.getAdvice() == null ? "" : medicationCatalog.getAdvice().trim().toLowerCase(Locale.ROOT);
        if (!advice.isBlank()) {
            String extractedFromAdvice = extractAdviceFrequency(advice);
            if (extractedFromAdvice != null) {
                return extractedFromAdvice;
            }
        }

        String normalizedName = medicationMapper.extractCatalogName(medicationCatalog).toLowerCase(Locale.ROOT);
        if (normalizedName.contains("inhaler") || normalizedName.contains("metered dose")) {
            return "2x daily";
        }
        if (normalizedName.contains("gel")
                || normalizedName.contains("cream")
                || normalizedName.contains("ointment")
                || normalizedName.contains("topical")) {
            return "Apply 2x daily";
        }
        if (normalizedName.contains("injection")
                || normalizedName.contains("prefilled syringe")
                || normalizedName.contains("transdermal")
                || normalizedName.contains("vaginal system")
                || normalizedName.contains("pack")) {
            return "As prescribed";
        }
        return "As prescribed";
    }

    private String extractAdviceFrequency(String advice) {
        if (advice.contains("4x") || advice.contains("four times")) {
            return "4x daily";
        }
        if (advice.contains("3x") || advice.contains("three times")) {
            return "3x daily";
        }
        if (advice.contains("2x") || advice.contains("twice")) {
            return "2x daily";
        }
        if (advice.contains("once") || advice.contains("1x")) {
            return "1x daily";
        }
        return null;
    }
}

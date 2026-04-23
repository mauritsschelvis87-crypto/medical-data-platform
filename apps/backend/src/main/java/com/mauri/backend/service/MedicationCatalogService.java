package com.mauri.backend.service;

import com.mauri.backend.dto.medication.MedicationCatalogDto;
import com.mauri.backend.mapper.MedicationMapper;
import com.mauri.backend.repository.MedicationCatalogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicationCatalogService {

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

    public List<MedicationCatalogDto> search(String query) {
        medicationCatalogImportService.ensureCatalogLoaded();

        String normalizedQuery = query == null ? "" : query.trim();

        return medicationCatalogRepository
                .findTop20ByActiveTrueAndDutchNameContainingIgnoreCaseOrActiveTrueAndLatinNameContainingIgnoreCaseOrderByDutchNameAsc(
                        normalizedQuery,
                        normalizedQuery
                )
                .stream()
                .map(medicationMapper::toCatalogDto)
                .toList();
    }
}

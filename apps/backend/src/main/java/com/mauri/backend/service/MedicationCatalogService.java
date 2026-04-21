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

    public MedicationCatalogService(MedicationCatalogRepository medicationCatalogRepository,
                                    MedicationMapper medicationMapper) {
        this.medicationCatalogRepository = medicationCatalogRepository;
        this.medicationMapper = medicationMapper;
    }

    public List<MedicationCatalogDto> search(String query) {
        if (query == null || query.isBlank()) {
            return medicationCatalogRepository.findByActiveTrueOrderByDutchNameAsc()
                    .stream()
                    .limit(20)
                    .map(medicationMapper::toCatalogDto)
                    .toList();
        }

        return medicationCatalogRepository
                .findTop20ByActiveTrueAndDutchNameContainingIgnoreCaseOrActiveTrueAndLatinNameContainingIgnoreCaseOrderByDutchNameAsc(
                        query.trim(),
                        query.trim()
                )
                .stream()
                .map(medicationMapper::toCatalogDto)
                .toList();
    }
}

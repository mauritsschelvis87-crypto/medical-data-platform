package com.mauri.backend.controller;

import com.mauri.backend.dto.medication.MedicationCatalogSearchResultDto;
import com.mauri.backend.dto.medication.MedicationCatalogSelectionDto;
import com.mauri.backend.service.MedicationCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationCatalogService medicationCatalogService;

    public MedicationController(MedicationCatalogService medicationCatalogService) {
        this.medicationCatalogService = medicationCatalogService;
    }

    @GetMapping("/search")
    public List<MedicationCatalogSearchResultDto> search(@RequestParam(required = false, name = "q") String query) {
        return medicationCatalogService.search(query);
    }

    @GetMapping("/{medicationId}")
    public MedicationCatalogSelectionDto getMedicationSelection(@PathVariable UUID medicationId) {
        return medicationCatalogService.getSelection(medicationId);
    }
}

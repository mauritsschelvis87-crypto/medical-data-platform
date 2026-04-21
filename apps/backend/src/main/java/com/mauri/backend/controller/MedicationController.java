package com.mauri.backend.controller;

import com.mauri.backend.dto.medication.MedicationCatalogDto;
import com.mauri.backend.service.MedicationCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationCatalogService medicationCatalogService;

    public MedicationController(MedicationCatalogService medicationCatalogService) {
        this.medicationCatalogService = medicationCatalogService;
    }

    @GetMapping("/search")
    public List<MedicationCatalogDto> search(@RequestParam(required = false, name = "q") String query) {
        return medicationCatalogService.search(query);
    }
}

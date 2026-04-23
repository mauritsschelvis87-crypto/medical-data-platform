package com.mauri.backend.controller;

import com.mauri.backend.dto.dataset.CreateDatasetImportRequest;
import com.mauri.backend.dto.dataset.DatasetImportDto;
import com.mauri.backend.service.DatasetImportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dataset-imports")
public class DatasetImportController {

    private final DatasetImportService datasetImportService;

    public DatasetImportController(DatasetImportService datasetImportService) {
        this.datasetImportService = datasetImportService;
    }

    @PostMapping
    public DatasetImportDto importDataset(@Valid @RequestBody CreateDatasetImportRequest request) {
        return datasetImportService.importNormalizedDataset(request);
    }

    @GetMapping
    public List<DatasetImportDto> getImports() {
        return datasetImportService.getAllImports();
    }

    @GetMapping("/{importId}")
    public DatasetImportDto getImportById(@PathVariable UUID importId) {
        return datasetImportService.getImportById(importId);
    }
}

package com.mauri.backend.controller;

import com.mauri.backend.dto.dataset.CreateDatasetImportRequest;
import com.mauri.backend.dto.dataset.DatasetImportDto;
import com.mauri.backend.service.DatasetImportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    private final DatasetImportService datasetImportService;

    public DatasetController(DatasetImportService datasetImportService) {
        this.datasetImportService = datasetImportService;
    }

    @PostMapping("/import")
    public DatasetImportDto registerImport(@RequestBody CreateDatasetImportRequest request) {
        return datasetImportService.registerImport(request);
    }

    @GetMapping("/imports")
    public List<DatasetImportDto> getImports() {
        return datasetImportService.getAllImports();
    }

    @GetMapping("/imports/{importId}")
    public DatasetImportDto getImportById(@PathVariable Long importId) {
        return datasetImportService.getImportById(importId);
    }
}

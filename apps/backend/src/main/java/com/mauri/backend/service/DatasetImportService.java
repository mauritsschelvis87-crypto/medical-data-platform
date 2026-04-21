package com.mauri.backend.service;

import com.mauri.backend.dto.dataset.CreateDatasetImportRequest;
import com.mauri.backend.dto.dataset.DatasetImportDto;
import com.mauri.backend.entity.DatasetImport;
import com.mauri.backend.enums.DatasetImportStatus;
import com.mauri.backend.exception.ResourceNotFoundException;
import com.mauri.backend.mapper.DatasetImportMapper;
import com.mauri.backend.repository.DatasetImportRepository;
import com.mauri.backend.service.dataset.SyntheaCsvImportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DatasetImportService {

    private final DatasetImportRepository datasetImportRepository;
    private final DatasetImportMapper datasetImportMapper;
    private final SyntheaCsvImportService syntheaCsvImportService;

    public DatasetImportService(DatasetImportRepository datasetImportRepository,
                                DatasetImportMapper datasetImportMapper,
                                SyntheaCsvImportService syntheaCsvImportService) {
        this.datasetImportRepository = datasetImportRepository;
        this.datasetImportMapper = datasetImportMapper;
        this.syntheaCsvImportService = syntheaCsvImportService;
    }

    public List<DatasetImportDto> getAllImports() {
        return datasetImportRepository.findAllByOrderByStartedAtDesc()
                .stream()
                .map(datasetImportMapper::toDto)
                .toList();
    }

    public DatasetImportDto getImportById(Long importId) {
        DatasetImport datasetImport = datasetImportRepository.findById(importId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset import not found with id: " + importId));
        return datasetImportMapper.toDto(datasetImport);
    }

    @Transactional
    public DatasetImportDto registerImport(CreateDatasetImportRequest request) {
        if (shouldImportSynthea(request)) {
            return datasetImportMapper.toDto(syntheaCsvImportService.importDataset(request));
        }

        DatasetImport datasetImport = new DatasetImport();
        datasetImport.setDatasetName(request.getDatasetName());
        datasetImport.setSourceFileName(buildSourceName(request));
        datasetImport.setImportStatus(DatasetImportStatus.COMPLETED);
        datasetImport.setRecordsReceived(defaultCount(request.getRecordCount()));
        datasetImport.setRecordsProcessed(defaultCount(request.getRecordCount()));
        datasetImport.setRecordsFailed(0);
        datasetImport.setStartedAt(LocalDateTime.now());
        datasetImport.setFinishedAt(LocalDateTime.now());
        datasetImport.setChecksum(request.getChecksum());
        datasetImport.setVersionTag(buildVersionTag(request));

        DatasetImport savedImport = datasetImportRepository.save(datasetImport);
        return datasetImportMapper.toDto(savedImport);
    }

    private String buildSourceName(CreateDatasetImportRequest request) {
        if (request.getSourceName() != null && !request.getSourceName().isBlank()) {
            return request.getSourceName().trim();
        }
        if (request.getImportType() != null && !request.getImportType().isBlank()) {
            return request.getImportType().trim();
        }
        return "manual-import";
    }

    private String buildVersionTag(CreateDatasetImportRequest request) {
        if (request.getVersionTag() != null && !request.getVersionTag().isBlank()) {
            return request.getVersionTag().trim();
        }
        if (request.getNormalizationVersion() != null && !request.getNormalizationVersion().isBlank()) {
            return request.getNormalizationVersion().trim();
        }
        return "v1";
    }

    private int defaultCount(Integer count) {
        return count != null ? count : 0;
    }

    private boolean shouldImportSynthea(CreateDatasetImportRequest request) {
        if (request.getSourcePath() == null || request.getSourcePath().isBlank()) {
            return false;
        }

        if (request.getImportType() == null || request.getImportType().isBlank()) {
            return true;
        }

        String normalizedType = request.getImportType().trim().toUpperCase();
        return normalizedType.equals("SYNTHEA")
                || normalizedType.equals("SYNTHEA_CSV")
                || normalizedType.equals("CSV");
    }
}

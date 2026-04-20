package com.mauri.backend.mapper;

import com.mauri.backend.dto.dataset.DatasetImportDto;
import com.mauri.backend.entity.DatasetImport;
import org.springframework.stereotype.Component;

@Component
public class DatasetImportMapper {

    public DatasetImportDto toDto(DatasetImport datasetImport) {
        if (datasetImport == null) {
            return null;
        }

        DatasetImportDto dto = new DatasetImportDto();
        dto.setId(datasetImport.getId());
        dto.setDatasetName(datasetImport.getDatasetName());
        dto.setImportStatus(datasetImport.getImportStatus() != null ? datasetImport.getImportStatus().name() : null);
        dto.setRecordsReceived(datasetImport.getRecordsReceived());
        dto.setRecordsProcessed(datasetImport.getRecordsProcessed());
        dto.setRecordsFailed(datasetImport.getRecordsFailed());
        dto.setStartedAt(datasetImport.getStartedAt());
        dto.setFinishedAt(datasetImport.getFinishedAt());

        return dto;
    }
}
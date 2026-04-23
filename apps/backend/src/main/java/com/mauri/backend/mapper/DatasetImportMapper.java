package com.mauri.backend.mapper;

import com.mauri.backend.dto.dataset.DatasetImportDto;
import com.mauri.backend.entity.DatasetImport;
import org.springframework.stereotype.Component;

@Component
public class DatasetImportMapper {

    public DatasetImportDto toDto(DatasetImport datasetImport) {
        return toDto(datasetImport, null);
    }

    public DatasetImportDto toDto(DatasetImport datasetImport, ImportSummary summary) {
        if (datasetImport == null) {
            return null;
        }

        DatasetImportDto dto = new DatasetImportDto();
        dto.setId(datasetImport.getId());
        dto.setSourceName(datasetImport.getSourceName());
        dto.setDatasetType(datasetImport.getDatasetType() != null ? datasetImport.getDatasetType().name() : null);
        dto.setStatus(datasetImport.getStatus() != null ? datasetImport.getStatus().name() : null);
        dto.setImportedAt(datasetImport.getImportedAt());
        dto.setNotes(datasetImport.getNotes());
        if (summary != null) {
            dto.setRecordsReceived(summary.recordsReceived());
            dto.setRecordsProcessed(summary.recordsProcessed());
            dto.setRecordsFailed(summary.recordsFailed());
            dto.setPatientCount(summary.patientCount());
            dto.setPatientAddressCount(summary.patientAddressCount());
            dto.setVitalSignsCount(summary.vitalSignsCount());
            dto.setSkippedRecords(summary.skippedRecords());
            dto.setValidationSummary(summary.validationSummary());
        }
        return dto;
    }

    public record ImportSummary(
            int recordsReceived,
            int recordsProcessed,
            int recordsFailed,
            int patientCount,
            int patientAddressCount,
            int vitalSignsCount,
            int skippedRecords,
            String validationSummary
    ) {
    }
}

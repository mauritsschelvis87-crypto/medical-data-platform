package com.mauri.backend.service;

import com.mauri.backend.dto.dataset.CreateDatasetImportRequest;
import com.mauri.backend.dto.dataset.DatasetImportDto;
import com.mauri.backend.entity.DatasetImport;
import com.mauri.backend.enums.DatasetImportStatus;
import com.mauri.backend.enums.DatasetType;
import com.mauri.backend.exception.CsvImportProcessingException;
import com.mauri.backend.exception.ResourceNotFoundException;
import com.mauri.backend.mapper.DatasetImportMapper;
import com.mauri.backend.repository.DatasetImportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DatasetImportService {

    private final DatasetImportRepository datasetImportRepository;
    private final DatasetImportMapper datasetImportMapper;
    private final DatasetImportPersistenceService datasetImportPersistenceService;
    private final PatientImportService patientImportService;
    private final PatientAddressImportService patientAddressImportService;
    private final VitalSignsImportService vitalSignsImportService;
    private final ImportSummaryService importSummaryService;

    public DatasetImportService(DatasetImportRepository datasetImportRepository,
                                DatasetImportMapper datasetImportMapper,
                                DatasetImportPersistenceService datasetImportPersistenceService,
                                PatientImportService patientImportService,
                                PatientAddressImportService patientAddressImportService,
                                VitalSignsImportService vitalSignsImportService,
                                ImportSummaryService importSummaryService) {
        this.datasetImportRepository = datasetImportRepository;
        this.datasetImportMapper = datasetImportMapper;
        this.datasetImportPersistenceService = datasetImportPersistenceService;
        this.patientImportService = patientImportService;
        this.patientAddressImportService = patientAddressImportService;
        this.vitalSignsImportService = vitalSignsImportService;
        this.importSummaryService = importSummaryService;
    }

    public List<DatasetImportDto> getAllImports() {
        return datasetImportRepository.findAllByOrderByImportedAtDesc().stream().map(datasetImportMapper::toDto).toList();
    }

    public DatasetImportDto getImportById(UUID importId) {
        DatasetImport datasetImport = datasetImportRepository.findById(importId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset import not found with id: " + importId));
        return datasetImportMapper.toDto(datasetImport);
    }

    public DatasetImportDto importNormalizedDataset(CreateDatasetImportRequest request) {
        DatasetType datasetType = parseDatasetType(request.getDatasetType());
        boolean replaceExistingData = request.isReplaceExistingData();
        DatasetImport datasetImport = new DatasetImport();
        datasetImport.setSourceName(request.getSourceName().trim());
        datasetImport.setDatasetType(datasetType);
        datasetImport.setImportedAt(LocalDateTime.now());
        datasetImport.setStatus(DatasetImportStatus.RUNNING);
        datasetImport.setNotes(request.getNotes());
        datasetImport = datasetImportPersistenceService.save(datasetImport);

        try {
            ImportSummaryService.ImportSummary importSummary = importSummaryService
                    .readSummary(request.getSourceDirectoryPath())
                    .orElse(null);

            PatientImportService.ImportResult patientResult =
                    patientImportService.importPatients(request.getSourceDirectoryPath(), datasetImport, replaceExistingData);
            PatientAddressImportService.ImportResult addressResult =
                    patientAddressImportService.importAddresses(request.getSourceDirectoryPath(), datasetImport, replaceExistingData);
            VitalSignsImportService.ImportResult vitalSignsResult =
                    vitalSignsImportService.importVitalSigns(request.getSourceDirectoryPath(), datasetImport, replaceExistingData);

            int recordsReceived = coalesce(importSummary != null ? importSummary.recordsReceived() : null,
                    patientResult.recordsReceived() + addressResult.recordsReceived() + vitalSignsResult.recordsReceived());
            int recordsProcessed = coalesce(importSummary != null ? importSummary.recordsProcessed() : null,
                    patientResult.recordsProcessed() + addressResult.recordsProcessed() + vitalSignsResult.recordsProcessed());
            int recordsFailed = coalesce(importSummary != null ? importSummary.recordsFailed() : null,
                    patientResult.recordsFailed() + addressResult.recordsFailed() + vitalSignsResult.recordsFailed());
            int skippedRecords = coalesce(importSummary != null ? importSummary.skippedRecords() : null,
                    patientResult.skippedRecords() + addressResult.skippedRecords() + vitalSignsResult.skippedRecords());
            int patientCount = coalesce(importSummary != null ? importSummary.patientCount() : null, patientResult.importedCount());
            int patientAddressCount = coalesce(importSummary != null ? importSummary.patientAddressCount() : null, addressResult.importedCount());
            int vitalSignsCount = coalesce(importSummary != null ? importSummary.vitalSignsCount() : null, vitalSignsResult.importedCount());

            datasetImport.setStatus(recordsFailed > 0 ? DatasetImportStatus.PARTIALLY_SUCCEEDED : DatasetImportStatus.SUCCEEDED);
            datasetImport.setNotes(buildSuccessNotes(
                    request.getNotes(),
                    patientCount,
                    patientAddressCount,
                    vitalSignsCount,
                    importSummary,
                    replaceExistingData
            ));
            datasetImport = datasetImportPersistenceService.save(datasetImport);

            return datasetImportMapper.toDto(
                    datasetImport,
                    new DatasetImportMapper.ImportSummary(
                            recordsReceived,
                            recordsProcessed,
                            recordsFailed,
                            patientCount,
                            patientAddressCount,
                            vitalSignsCount,
                            skippedRecords,
                            buildValidationSummary(patientResult, addressResult, vitalSignsResult, importSummary)
                    )
            );
        } catch (RuntimeException exception) {
            datasetImport.setStatus(DatasetImportStatus.FAILED);
            datasetImport.setNotes(buildFailureNotes(request.getNotes(), exception));
            datasetImportPersistenceService.save(datasetImport);
            throw new CsvImportProcessingException("Dataset import failed: " + exception.getMessage(), exception);
        }
    }

    private DatasetType parseDatasetType(String rawDatasetType) {
        try {
            return DatasetType.valueOf(rawDatasetType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new CsvImportProcessingException("Unsupported datasetType: " + rawDatasetType, exception);
        }
    }

    private String buildSuccessNotes(String requestNotes,
                                     int patientCount,
                                     int patientAddressCount,
                                     int vitalSignsCount,
                                     ImportSummaryService.ImportSummary importSummary,
                                     boolean replaceExistingData) {
        StringBuilder notes = new StringBuilder();
        if (requestNotes != null && !requestNotes.isBlank()) {
            notes.append(requestNotes.trim()).append(System.lineSeparator());
        }
        notes.append("Replace existing data: ").append(replaceExistingData).append(System.lineSeparator());
        notes.append("Patients imported: ").append(patientCount).append(System.lineSeparator());
        notes.append("Addresses imported: ").append(patientAddressCount).append(System.lineSeparator());
        notes.append("Vital signs imported: ").append(vitalSignsCount);
        if (importSummary != null && importSummary.validationSummary() != null && !importSummary.validationSummary().isBlank()) {
            notes.append(System.lineSeparator()).append("Import summary: ").append(importSummary.validationSummary().trim());
        }
        return notes.toString();
    }

    private String buildFailureNotes(String requestNotes, RuntimeException exception) {
        if (requestNotes == null || requestNotes.isBlank()) {
            return exception.getMessage();
        }
        return requestNotes.trim() + System.lineSeparator() + exception.getMessage();
    }

    private String buildValidationSummary(PatientImportService.ImportResult patientResult,
                                          PatientAddressImportService.ImportResult addressResult,
                                          VitalSignsImportService.ImportResult vitalSignsResult,
                                          ImportSummaryService.ImportSummary importSummary) {
        String baseSummary = "Patients failed=%d, addresses failed=%d, vitals failed=%d"
                .formatted(patientResult.recordsFailed(), addressResult.recordsFailed(), vitalSignsResult.recordsFailed());
        if (importSummary == null || importSummary.validationSummary() == null || importSummary.validationSummary().isBlank()) {
            return baseSummary;
        }
        return baseSummary + "; source summary=" + importSummary.validationSummary().trim();
    }

    private int coalesce(Integer preferred, int fallback) {
        return preferred != null ? preferred : fallback;
    }
}

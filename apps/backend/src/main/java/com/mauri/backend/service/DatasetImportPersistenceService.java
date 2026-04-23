package com.mauri.backend.service;

import com.mauri.backend.entity.DatasetImport;
import com.mauri.backend.repository.DatasetImportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatasetImportPersistenceService {

    private final DatasetImportRepository datasetImportRepository;

    public DatasetImportPersistenceService(DatasetImportRepository datasetImportRepository) {
        this.datasetImportRepository = datasetImportRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DatasetImport save(DatasetImport datasetImport) {
        return datasetImportRepository.save(datasetImport);
    }
}

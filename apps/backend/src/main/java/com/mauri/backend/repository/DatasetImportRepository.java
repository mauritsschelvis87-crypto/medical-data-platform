package com.mauri.backend.repository;

import com.mauri.backend.entity.DatasetImport;
import com.mauri.backend.enums.DatasetImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetImportRepository extends JpaRepository<DatasetImport, Long> {

    List<DatasetImport> findByDatasetNameOrderByStartedAtDesc(String datasetName);

    List<DatasetImport> findByImportStatusOrderByStartedAtDesc(DatasetImportStatus importStatus);
}
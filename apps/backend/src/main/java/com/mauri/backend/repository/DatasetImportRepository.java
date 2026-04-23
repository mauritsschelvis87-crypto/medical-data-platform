package com.mauri.backend.repository;

import com.mauri.backend.entity.DatasetImport;
import com.mauri.backend.enums.DatasetImportStatus;
import com.mauri.backend.enums.DatasetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DatasetImportRepository extends JpaRepository<DatasetImport, UUID> {

    List<DatasetImport> findBySourceNameOrderByImportedAtDesc(String sourceName);

    List<DatasetImport> findByDatasetTypeOrderByImportedAtDesc(DatasetType datasetType);

    List<DatasetImport> findByStatusOrderByImportedAtDesc(DatasetImportStatus status);

    List<DatasetImport> findAllByOrderByImportedAtDesc();
}

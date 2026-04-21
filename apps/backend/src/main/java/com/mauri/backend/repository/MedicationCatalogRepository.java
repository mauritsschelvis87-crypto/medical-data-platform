package com.mauri.backend.repository;

import com.mauri.backend.entity.MedicationCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationCatalogRepository extends JpaRepository<MedicationCatalog, Long> {

    Optional<MedicationCatalog> findByCode(String code);

    boolean existsByCode(String code);

    List<MedicationCatalog> findByDutchNameContainingIgnoreCase(String dutchName);

    List<MedicationCatalog> findByLatinNameContainingIgnoreCase(String latinName);

    List<MedicationCatalog> findByActiveTrueOrderByDutchNameAsc();

    List<MedicationCatalog> findTop20ByActiveTrueAndDutchNameContainingIgnoreCaseOrActiveTrueAndLatinNameContainingIgnoreCaseOrderByDutchNameAsc(
            String dutchName,
            String latinName
    );
}

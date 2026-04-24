package com.mauri.backend.repository;

import com.mauri.backend.entity.MedicationCatalog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicationCatalogRepository extends JpaRepository<MedicationCatalog, UUID> {

    Optional<MedicationCatalog> findByCode(String code);

    boolean existsByCode(String code);

    List<MedicationCatalog> findByDutchNameContainingIgnoreCase(String dutchName);

    List<MedicationCatalog> findByLatinNameContainingIgnoreCase(String latinName);

    long countByActiveTrue();

    List<MedicationCatalog> findByActiveTrueOrderByDutchNameAsc();

    @Query("""
            select medicationCatalog
            from MedicationCatalog medicationCatalog
            where medicationCatalog.active = true
              and (
                  lower(medicationCatalog.dutchName) like lower(concat('%', :query, '%'))
                  or lower(coalesce(medicationCatalog.latinName, '')) like lower(concat('%', :query, '%'))
                  or lower(coalesce(medicationCatalog.code, '')) like lower(concat('%', :query, '%'))
              )
            order by medicationCatalog.dutchName asc
            """)
    List<MedicationCatalog> searchActiveCatalog(@Param("query") String query, Pageable pageable);
}

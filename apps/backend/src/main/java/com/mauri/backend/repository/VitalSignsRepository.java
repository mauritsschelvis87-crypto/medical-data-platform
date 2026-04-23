package com.mauri.backend.repository;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.VitalSignSource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VitalSignsRepository extends JpaRepository<VitalSigns, UUID> {

    List<VitalSigns> findByPatientOrderByMeasuredAtDesc(Patient patient);

    List<VitalSigns> findTop10ByPatientOrderByMeasuredAtDesc(Patient patient);

    @Query("""
            select vitalSigns
            from VitalSigns vitalSigns
            where vitalSigns.patient = :patient
              and vitalSigns.measuredAt = (
                  select max(latest.measuredAt)
                  from VitalSigns latest
                  where latest.patient = :patient
                    and latest.type = vitalSigns.type
              )
            order by vitalSigns.type asc
            """)
    List<VitalSigns> findLatestPerTypeByPatient(Patient patient);

    VitalSigns findFirstByPatientAndTypeOrderByMeasuredAtDesc(Patient patient, com.mauri.backend.enums.VitalSignType type);

    void deleteByPatient(Patient patient);

    void deleteByPatientAndSource(Patient patient, VitalSignSource source);
}

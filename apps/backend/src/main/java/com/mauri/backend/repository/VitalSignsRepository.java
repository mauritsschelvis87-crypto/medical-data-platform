package com.mauri.backend.repository;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.VitalSigns;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VitalSignsRepository extends JpaRepository<VitalSigns, Long> {

    List<VitalSigns> findByPatientOrderByMeasuredAtDesc(Patient patient);

    List<VitalSigns> findTop10ByPatientOrderByMeasuredAtDesc(Patient patient);
}
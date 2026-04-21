package com.mauri.backend.repository;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.PatientMedication;
import com.mauri.backend.enums.MedicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientMedicationRepository extends JpaRepository<PatientMedication, Long> {

    List<PatientMedication> findByPatientOrderByStartDateDesc(Patient patient);

    List<PatientMedication> findByPatientAndStatusOrderByStartDateDesc(
            Patient patient,
            MedicationStatus status
    );

    List<PatientMedication> findByPatientAndStatusInOrderByStartDateDesc(
            Patient patient,
            List<MedicationStatus> statuses
    );
}

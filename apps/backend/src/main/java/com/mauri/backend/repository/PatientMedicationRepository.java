package com.mauri.backend.repository;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.PatientMedication;
import com.mauri.backend.enums.MedicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientMedicationRepository extends JpaRepository<PatientMedication, UUID> {

    List<PatientMedication> findByPatientOrderByStartDateDesc(Patient patient);

    List<PatientMedication> findByPatientAndStatusOrderByStartDateDesc(
            Patient patient,
            MedicationStatus status
    );

    List<PatientMedication> findByPatientAndStatusInOrderByStartDateDesc(
            Patient patient,
            List<MedicationStatus> statuses
    );

    void deleteByPatient(Patient patient);
}

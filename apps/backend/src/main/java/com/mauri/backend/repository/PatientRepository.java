package com.mauri.backend.repository;

import com.mauri.backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByPatientNumber(String patientNumber);

    Optional<Patient> findBySourcePatientId(String sourcePatientId);

    boolean existsByPatientNumber(String patientNumber);

    boolean existsBySourcePatientId(String sourcePatientId);

    List<Patient> findByBirthDate(LocalDate birthDate);

    List<Patient> findByLastNameContainingIgnoreCase(String lastName);

    List<Patient> findByFirstNameContainingIgnoreCase(String firstName);

    List<Patient> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(String firstName, String lastName);

    List<Patient> findTop20ByPatientNumberContainingIgnoreCaseOrderByPatientNumberAsc(String patientNumber);

    List<Patient> findTop20ByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrderByLastNameAscFirstNameAsc(
            String firstName,
            String lastName
    );

    List<Patient> findTop20ByFullNameContainingIgnoreCaseOrderByFullNameAsc(String fullName);

    List<Patient> findAllByOrderByPatientNumberAsc();
}

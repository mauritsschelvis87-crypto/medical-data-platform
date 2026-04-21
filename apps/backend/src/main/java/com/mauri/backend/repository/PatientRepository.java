package com.mauri.backend.repository;

import com.mauri.backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientNumber(String patientNumber);

    boolean existsByPatientNumber(String patientNumber);

    List<Patient> findByLastNameContainingIgnoreCase(String lastName);

    List<Patient> findByFirstNameContainingIgnoreCase(String firstName);

    List<Patient> findByBirthDate(LocalDate birthDate);

    List<Patient> findTop20ByPatientNumberContainingIgnoreCaseOrderByPatientNumberAsc(String patientNumber);

    List<Patient> findTop20ByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrderByLastNameAscFirstNameAsc(
            String firstName,
            String lastName
    );

    List<Patient> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
            String firstName,
            String lastName
    );
}

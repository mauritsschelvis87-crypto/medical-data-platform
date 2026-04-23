package com.mauri.backend.repository;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.PatientAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientAddressRepository extends JpaRepository<PatientAddress, UUID> {

    Optional<PatientAddress> findByPatient(Patient patient);

    boolean existsByPatient(Patient patient);

    void deleteByPatient(Patient patient);

    void deleteAllByPatientIn(Iterable<Patient> patients);
}

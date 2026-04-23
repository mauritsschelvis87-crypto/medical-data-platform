package com.mauri.backend.repository;

import com.mauri.backend.entity.ConsultNote;
import com.mauri.backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsultNoteRepository extends JpaRepository<ConsultNote, UUID> {

    List<ConsultNote> findByPatientOrderByCreatedAtDesc(Patient patient);

    List<ConsultNote> findTop5ByPatientOrderByCreatedAtDesc(Patient patient);

    void deleteByPatient(Patient patient);
}

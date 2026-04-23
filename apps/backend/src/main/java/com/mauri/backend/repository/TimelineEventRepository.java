package com.mauri.backend.repository;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimelineEventRepository extends JpaRepository<TimelineEvent, UUID> {

    List<TimelineEvent> findByPatientOrderByEventTimestampDesc(Patient patient);

    List<TimelineEvent> findTop50ByPatientOrderByEventTimestampDesc(Patient patient);

    void deleteByPatient(Patient patient);
}

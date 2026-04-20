package com.mauri.backend.repository;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimelineEventRepository extends JpaRepository<TimelineEvent, Long> {

    List<TimelineEvent> findByPatientOrderByEventTimestampDesc(Patient patient);

    List<TimelineEvent> findTop50ByPatientOrderByEventTimestampDesc(Patient patient);
}
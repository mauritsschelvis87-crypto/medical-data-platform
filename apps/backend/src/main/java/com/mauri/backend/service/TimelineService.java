package com.mauri.backend.service;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.TimelineEvent;
import com.mauri.backend.enums.TimelineEventType;
import com.mauri.backend.repository.TimelineEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TimelineService {

    private final TimelineEventRepository timelineEventRepository;

    public TimelineService(TimelineEventRepository timelineEventRepository) {
        this.timelineEventRepository = timelineEventRepository;
    }

    public void createEvent(Patient patient,
                            TimelineEventType eventType,
                            Long referenceId,
                            String referenceType,
                            String title,
                            String description,
                            LocalDateTime eventTimestamp) {
        TimelineEvent timelineEvent = new TimelineEvent();
        timelineEvent.setPatient(patient);
        timelineEvent.setEventType(eventType);
        timelineEvent.setReferenceId(referenceId);
        timelineEvent.setReferenceType(referenceType);
        timelineEvent.setTitle(title);
        timelineEvent.setDescription(description);
        timelineEvent.setEventTimestamp(eventTimestamp != null ? eventTimestamp : LocalDateTime.now());

        timelineEventRepository.save(timelineEvent);
    }
}
package com.mauri.backend.controller;

import com.mauri.backend.dto.timeline.TimelineEventDto;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.mapper.TimelineMapper;
import com.mauri.backend.repository.TimelineEventRepository;
import com.mauri.backend.service.PatientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients/{patientId}/timeline")
public class TimelineController {

    private final TimelineEventRepository timelineEventRepository;
    private final PatientService patientService;
    private final TimelineMapper timelineMapper;

    public TimelineController(TimelineEventRepository timelineEventRepository,
                              PatientService patientService,
                              TimelineMapper timelineMapper) {
        this.timelineEventRepository = timelineEventRepository;
        this.patientService = patientService;
        this.timelineMapper = timelineMapper;
    }

    @GetMapping
    public List<TimelineEventDto> getTimelineForPatient(@PathVariable UUID patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return timelineEventRepository.findTop50ByPatientOrderByEventTimestampDesc(patient)
                .stream()
                .map(timelineMapper::toDto)
                .toList();
    }
}

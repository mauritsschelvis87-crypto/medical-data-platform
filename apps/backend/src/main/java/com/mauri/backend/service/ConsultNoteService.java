package com.mauri.backend.service;

import com.mauri.backend.dto.consult.ConsultNoteDto;
import com.mauri.backend.dto.consult.CreateConsultNoteRequest;
import com.mauri.backend.entity.ConsultNote;
import com.mauri.backend.entity.ConsultNoteVersion;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.enums.ConsultNoteStatus;
import com.mauri.backend.enums.TimelineEventType;
import com.mauri.backend.mapper.ConsultNoteMapper;
import com.mauri.backend.repository.ConsultNoteRepository;
import com.mauri.backend.repository.ConsultNoteVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsultNoteService {

    private final ConsultNoteRepository consultNoteRepository;
    private final ConsultNoteVersionRepository consultNoteVersionRepository;
    private final PatientService patientService;
    private final ConsultNoteMapper consultNoteMapper;
    private final TimelineService timelineService;

    public ConsultNoteService(ConsultNoteRepository consultNoteRepository,
                              ConsultNoteVersionRepository consultNoteVersionRepository,
                              PatientService patientService,
                              ConsultNoteMapper consultNoteMapper,
                              TimelineService timelineService) {
        this.consultNoteRepository = consultNoteRepository;
        this.consultNoteVersionRepository = consultNoteVersionRepository;
        this.patientService = patientService;
        this.consultNoteMapper = consultNoteMapper;
        this.timelineService = timelineService;
    }

    public List<ConsultNoteDto> getConsultNotesForPatient(Long patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return consultNoteRepository.findByPatientOrderByCreatedAtDesc(patient)
                .stream()
                .map(consultNoteMapper::toDto)
                .toList();
    }

    @Transactional
    public ConsultNoteDto createConsultNote(Long patientId, CreateConsultNoteRequest request) {
        Patient patient = patientService.getPatientEntityById(patientId);

        ConsultNote consultNote = new ConsultNote();
        consultNote.setPatient(patient);
        consultNote.setCreatedBy(request.getCreatedBy());
        consultNote.setStatus(ConsultNoteStatus.FINALIZED);

        ConsultNote savedConsultNote = consultNoteRepository.save(consultNote);

        ConsultNoteVersion version = new ConsultNoteVersion();
        version.setConsultNote(savedConsultNote);
        version.setVersionNumber("1.0");
        version.setSubjective(request.getSubjective());
        version.setObjective(request.getObjective());
        version.setAssessment(request.getAssessment());
        version.setPlan(request.getPlan());
        version.setCreatedBy(request.getCreatedBy());
        version.setChangeReason(request.getChangeReason());

        ConsultNoteVersion savedVersion = consultNoteVersionRepository.save(version);

        savedConsultNote.setCurrentVersion(savedVersion);
        savedConsultNote.getVersions().add(savedVersion);

        ConsultNote updatedConsultNote = consultNoteRepository.save(savedConsultNote);

        timelineService.createEvent(
                patient,
                TimelineEventType.CONSULT_NOTE_CREATED,
                updatedConsultNote.getId(),
                "ConsultNote",
                "Consult note created",
                buildConsultDescription(request),
                updatedConsultNote.getCreatedAt()
        );

        return consultNoteMapper.toDto(updatedConsultNote);
    }

    private String buildConsultDescription(CreateConsultNoteRequest request) {
        if (request.getAssessment() != null && !request.getAssessment().isBlank()) {
            return "Assessment: " + request.getAssessment();
        }

        return "SOAP consult note added";
    }
}
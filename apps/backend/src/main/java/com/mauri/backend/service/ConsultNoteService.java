package com.mauri.backend.service;

import com.mauri.backend.dto.consult.ConsultNoteDto;
import com.mauri.backend.dto.consult.ConsultNoteVersionDto;
import com.mauri.backend.dto.consult.CreateConsultNoteRequest;
import com.mauri.backend.dto.consult.CreateConsultNoteVersionRequest;
import com.mauri.backend.entity.ConsultNote;
import com.mauri.backend.entity.ConsultNoteVersion;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.enums.ConsultNoteStatus;
import com.mauri.backend.enums.TimelineEventType;
import com.mauri.backend.exception.ResourceNotFoundException;
import com.mauri.backend.mapper.ConsultNoteMapper;
import com.mauri.backend.repository.ConsultNoteRepository;
import com.mauri.backend.repository.ConsultNoteVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConsultNoteService {

    private final ConsultNoteRepository consultNoteRepository;
    private final ConsultNoteVersionRepository consultNoteVersionRepository;
    private final PatientService patientService;
    private final ConsultNoteMapper consultNoteMapper;
    private final TimelineService timelineService;
    private final PredictionWorkflowService predictionWorkflowService;

    public ConsultNoteService(ConsultNoteRepository consultNoteRepository,
                              ConsultNoteVersionRepository consultNoteVersionRepository,
                              PatientService patientService,
                              ConsultNoteMapper consultNoteMapper,
                              TimelineService timelineService,
                              PredictionWorkflowService predictionWorkflowService) {
        this.consultNoteRepository = consultNoteRepository;
        this.consultNoteVersionRepository = consultNoteVersionRepository;
        this.patientService = patientService;
        this.consultNoteMapper = consultNoteMapper;
        this.timelineService = timelineService;
        this.predictionWorkflowService = predictionWorkflowService;
    }

    public List<ConsultNoteDto> getConsultNotesForPatient(UUID patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return consultNoteRepository.findByPatientOrderByCreatedAtDesc(patient)
                .stream()
                .map(consultNoteMapper::toDto)
                .toList();
    }

    @Transactional
    public ConsultNoteDto createConsultNote(UUID patientId, CreateConsultNoteRequest request) {
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
        predictionWorkflowService.recalculatePredictions(patientId, "CONSULT_NOTE_CREATED", updatedConsultNote.getId());

        return consultNoteMapper.toDto(updatedConsultNote);
    }

    public ConsultNoteDto getConsultNoteById(UUID consultNoteId) {
        return consultNoteMapper.toDto(getConsultNoteEntity(consultNoteId));
    }

    public List<ConsultNoteVersionDto> getVersionsForConsultNote(UUID consultNoteId) {
        ConsultNote consultNote = getConsultNoteEntity(consultNoteId);
        return consultNoteVersionRepository.findByConsultNoteOrderByCreatedAtDesc(consultNote)
                .stream()
                .map(consultNoteMapper::toVersionDto)
                .toList();
    }

    @Transactional
    public ConsultNoteDto addVersion(UUID consultNoteId, CreateConsultNoteVersionRequest request) {
        ConsultNote consultNote = getConsultNoteEntity(consultNoteId);

        ConsultNoteVersion version = new ConsultNoteVersion();
        version.setConsultNote(consultNote);
        version.setVersionNumber(nextVersionNumber(consultNote));
        version.setSubjective(request.getSubjective());
        version.setObjective(request.getObjective());
        version.setAssessment(request.getAssessment());
        version.setPlan(request.getPlan());
        version.setCreatedBy(request.getCreatedBy());
        version.setChangeReason(request.getChangeReason());

        ConsultNoteVersion savedVersion = consultNoteVersionRepository.save(version);
        consultNote.setCurrentVersion(savedVersion);
        consultNote.getVersions().add(savedVersion);

        ConsultNote updatedConsultNote = consultNoteRepository.save(consultNote);
        timelineService.createEvent(
                consultNote.getPatient(),
                TimelineEventType.CONSULT_NOTE_UPDATED,
                updatedConsultNote.getId(),
                "ConsultNote",
                "Consult note updated",
                buildVersionDescription(request),
                savedVersion.getCreatedAt()
        );
        predictionWorkflowService.recalculatePredictions(
                consultNote.getPatient().getId(),
                "CONSULT_NOTE_UPDATED",
                updatedConsultNote.getId()
        );
        return consultNoteMapper.toDto(updatedConsultNote);
    }

    private String buildConsultDescription(CreateConsultNoteRequest request) {
        if (request.getAssessment() != null && !request.getAssessment().isBlank()) {
            return "Assessment: " + request.getAssessment();
        }

        return "SOAP consult note added";
    }

    private ConsultNote getConsultNoteEntity(UUID consultNoteId) {
        return consultNoteRepository.findById(consultNoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Consult note not found with id: " + consultNoteId));
    }

    private String nextVersionNumber(ConsultNote consultNote) {
        int nextVersion = consultNote.getVersions().size() + 1;
        return nextVersion + ".0";
    }

    private String buildVersionDescription(CreateConsultNoteVersionRequest request) {
        if (request.getChangeReason() != null && !request.getChangeReason().isBlank()) {
            return "Reason: " + request.getChangeReason();
        }
        if (request.getAssessment() != null && !request.getAssessment().isBlank()) {
            return "Assessment updated: " + request.getAssessment();
        }
        return "Consult note version added";
    }
}

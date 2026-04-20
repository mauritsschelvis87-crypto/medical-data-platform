package com.mauri.backend.mapper;

import com.mauri.backend.dto.consult.ConsultNoteDto;
import com.mauri.backend.dto.consult.ConsultNoteVersionDto;
import com.mauri.backend.entity.ConsultNote;
import com.mauri.backend.entity.ConsultNoteVersion;
import org.springframework.stereotype.Component;

@Component
public class ConsultNoteMapper {

    public ConsultNoteDto toDto(ConsultNote consultNote) {
        if (consultNote == null) {
            return null;
        }

        ConsultNoteDto dto = new ConsultNoteDto();
        dto.setId(consultNote.getId());
        dto.setStatus(consultNote.getStatus() != null ? consultNote.getStatus().name() : null);
        dto.setCreatedBy(consultNote.getCreatedBy());
        dto.setCreatedAt(consultNote.getCreatedAt());
        dto.setCurrentVersion(toVersionDto(consultNote.getCurrentVersion()));

        return dto;
    }

    public ConsultNoteVersionDto toVersionDto(ConsultNoteVersion version) {
        if (version == null) {
            return null;
        }

        ConsultNoteVersionDto dto = new ConsultNoteVersionDto();
        dto.setId(version.getId());
        dto.setVersionNumber(version.getVersionNumber());
        dto.setSubjective(version.getSubjective());
        dto.setObjective(version.getObjective());
        dto.setAssessment(version.getAssessment());
        dto.setPlan(version.getPlan());
        dto.setCreatedBy(version.getCreatedBy());
        dto.setCreatedAt(version.getCreatedAt());

        return dto;
    }
}
package com.mauri.backend.controller;

import com.mauri.backend.dto.consult.ConsultNoteDto;
import com.mauri.backend.dto.consult.ConsultNoteVersionDto;
import com.mauri.backend.dto.consult.CreateConsultNoteVersionRequest;
import com.mauri.backend.service.ConsultNoteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/consult-notes")
public class ConsultNoteVersionController {

    private final ConsultNoteService consultNoteService;

    public ConsultNoteVersionController(ConsultNoteService consultNoteService) {
        this.consultNoteService = consultNoteService;
    }

    @GetMapping("/{consultNoteId}")
    public ConsultNoteDto getConsultNote(@PathVariable Long consultNoteId) {
        return consultNoteService.getConsultNoteById(consultNoteId);
    }

    @GetMapping("/{consultNoteId}/versions")
    public List<ConsultNoteVersionDto> getVersions(@PathVariable Long consultNoteId) {
        return consultNoteService.getVersionsForConsultNote(consultNoteId);
    }

    @PostMapping("/{consultNoteId}/versions")
    public ConsultNoteDto addVersion(@PathVariable Long consultNoteId,
                                     @RequestBody CreateConsultNoteVersionRequest request) {
        return consultNoteService.addVersion(consultNoteId, request);
    }
}

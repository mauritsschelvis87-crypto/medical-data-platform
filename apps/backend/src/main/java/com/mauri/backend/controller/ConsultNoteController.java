package com.mauri.backend.controller;

import com.mauri.backend.dto.consult.ConsultNoteDto;
import com.mauri.backend.dto.consult.CreateConsultNoteRequest;
import com.mauri.backend.service.ConsultNoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/consult-notes")
public class ConsultNoteController {

    private final ConsultNoteService consultNoteService;

    public ConsultNoteController(ConsultNoteService consultNoteService) {
        this.consultNoteService = consultNoteService;
    }

    @GetMapping
    public List<ConsultNoteDto> getConsultNotesForPatient(@PathVariable Long patientId) {
        return consultNoteService.getConsultNotesForPatient(patientId);
    }

    @PostMapping
    public ConsultNoteDto createConsultNote(@PathVariable Long patientId,
                                            @RequestBody CreateConsultNoteRequest request) {
        return consultNoteService.createConsultNote(patientId, request);
    }
}
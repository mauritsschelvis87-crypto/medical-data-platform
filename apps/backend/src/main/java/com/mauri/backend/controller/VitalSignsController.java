package com.mauri.backend.controller;

import com.mauri.backend.dto.vitals.CreateVitalSignsRequest;
import com.mauri.backend.dto.vitals.VitalSignsDto;
import com.mauri.backend.service.VitalSignsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/patients/{patientId}/vital-signs", "/api/patients/{patientId}/vitals"})
public class VitalSignsController {

    private final VitalSignsService vitalSignsService;

    public VitalSignsController(VitalSignsService vitalSignsService) {
        this.vitalSignsService = vitalSignsService;
    }

    @GetMapping
    public List<VitalSignsDto> getVitalSignsForPatient(@PathVariable UUID patientId) {
        return vitalSignsService.getVitalSignsForPatient(patientId);
    }

    @GetMapping("/latest")
    public List<VitalSignsDto> getLatestVitalSignsForPatient(@PathVariable UUID patientId) {
        return vitalSignsService.getLatestVitalSignsForPatient(patientId);
    }

    @PostMapping
    public VitalSignsDto createVitalSigns(@PathVariable UUID patientId,
                                          @Valid @RequestBody CreateVitalSignsRequest request) {
        return vitalSignsService.createVitalSigns(patientId, request);
    }
}

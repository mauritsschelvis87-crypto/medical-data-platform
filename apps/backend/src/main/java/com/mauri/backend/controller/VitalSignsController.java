package com.mauri.backend.controller;

import com.mauri.backend.dto.vitals.CreateVitalSignsRequest;
import com.mauri.backend.dto.vitals.VitalSignsDto;
import com.mauri.backend.service.VitalSignsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/vital-signs")
public class VitalSignsController {

    private final VitalSignsService vitalSignsService;

    public VitalSignsController(VitalSignsService vitalSignsService) {
        this.vitalSignsService = vitalSignsService;
    }

    @GetMapping
    public List<VitalSignsDto> getVitalSignsForPatient(@PathVariable Long patientId) {
        return vitalSignsService.getVitalSignsForPatient(patientId);
    }

    @GetMapping("/latest")
    public List<VitalSignsDto> getLatestVitalSignsForPatient(@PathVariable Long patientId) {
        return vitalSignsService.getLatestVitalSignsForPatient(patientId);
    }

    @PostMapping
    public VitalSignsDto createVitalSigns(@PathVariable Long patientId,
                                          @RequestBody CreateVitalSignsRequest request) {
        return vitalSignsService.createVitalSigns(patientId, request);
    }
}
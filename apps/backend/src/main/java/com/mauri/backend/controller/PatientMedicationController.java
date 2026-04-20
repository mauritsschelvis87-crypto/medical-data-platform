package com.mauri.backend.controller;

import com.mauri.backend.dto.medication.CreatePatientMedicationRequest;
import com.mauri.backend.dto.medication.PatientMedicationDto;
import com.mauri.backend.enums.MedicationStatus;
import com.mauri.backend.service.PatientMedicationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/medications")
public class PatientMedicationController {

    private final PatientMedicationService patientMedicationService;

    public PatientMedicationController(PatientMedicationService patientMedicationService) {
        this.patientMedicationService = patientMedicationService;
    }

    @GetMapping
    public List<PatientMedicationDto> getMedicationsForPatient(@PathVariable Long patientId) {
        return patientMedicationService.getMedicationsForPatient(patientId);
    }

    @GetMapping("/by-status")
    public List<PatientMedicationDto> getMedicationsForPatientByStatus(
            @PathVariable Long patientId,
            @RequestParam MedicationStatus status
    ) {
        return patientMedicationService.getMedicationsForPatientByStatus(patientId, status);
    }

    @PostMapping
    public PatientMedicationDto createPatientMedication(@PathVariable Long patientId,
                                                        @RequestBody CreatePatientMedicationRequest request) {
        return patientMedicationService.createPatientMedication(patientId, request);
    }
}
package com.mauri.backend.controller;

import com.mauri.backend.dto.patient.PatientDto;
import com.mauri.backend.dto.patient.PatientSearchResultDto;
import com.mauri.backend.dto.patient.UpdatePatientAddressRequest;
import com.mauri.backend.service.PatientService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/patients", produces = MediaType.APPLICATION_JSON_VALUE)
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public List<PatientSearchResultDto> getInitialPatients() {
        return patientService.getInitialPatients();
    }

    @GetMapping("/{patientId}")
    public PatientDto getPatientById(@PathVariable UUID patientId) {
        return patientService.getPatientById(patientId);
    }

    @PutMapping("/{patientId}/address")
    public PatientDto updatePatientAddress(
            @PathVariable UUID patientId,
            @RequestBody UpdatePatientAddressRequest request
    ) {
        return patientService.updatePatientAddress(patientId, request);
    }

    @GetMapping("/search")
    public List<PatientSearchResultDto> search(@RequestParam("q") String query) {
        return patientService.search(query);
    }

    @GetMapping("/search/by-patient-number")
    public List<PatientSearchResultDto> searchByPatientNumber(@RequestParam String patientNumber) {
        return patientService.searchByPatientNumber(patientNumber);
    }

    @GetMapping("/search/by-birth-date")
    public List<PatientSearchResultDto> searchByBirthDate(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate
    ) {
        return patientService.searchByBirthDate(birthDate);
    }

    @GetMapping("/search/by-last-name")
    public List<PatientSearchResultDto> searchByLastName(@RequestParam String lastName) {
        return patientService.searchByLastName(lastName);
    }

    @GetMapping("/search/by-first-name")
    public List<PatientSearchResultDto> searchByFirstName(@RequestParam String firstName) {
        return patientService.searchByFirstName(firstName);
    }

    @GetMapping("/search/by-full-name")
    public List<PatientSearchResultDto> searchByFullName(
            @RequestParam String firstName,
            @RequestParam String lastName
    ) {
        return patientService.searchByFullName(firstName, lastName);
    }
}

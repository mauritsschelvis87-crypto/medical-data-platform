package com.mauri.backend.controller;

import com.mauri.backend.dto.patient.PatientDto;
import com.mauri.backend.dto.patient.PatientSearchResultDto;
import com.mauri.backend.service.PatientService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/{patientId}")
    public PatientDto getPatientById(@PathVariable Long patientId) {
        return patientService.getPatientById(patientId);
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
package com.mauri.backend.service;

import com.mauri.backend.dto.patient.PatientDto;
import com.mauri.backend.dto.patient.PatientSearchResultDto;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.exception.ResourceNotFoundException;
import com.mauri.backend.mapper.PatientMapper;
import com.mauri.backend.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    public PatientService(PatientRepository patientRepository, PatientMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
    }

    public PatientDto getPatientById(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        return patientMapper.toDto(patient);
    }

    public List<PatientSearchResultDto> searchByPatientNumber(String patientNumber) {
        return patientRepository.findByPatientNumber(patientNumber)
                .map(patient -> List.of(patientMapper.toSearchResultDto(patient)))
                .orElse(List.of());
    }

    public List<PatientSearchResultDto> searchByBirthDate(LocalDate birthDate) {
        return patientRepository.findByBirthDate(birthDate)
                .stream()
                .map(patientMapper::toSearchResultDto)
                .toList();
    }

    public List<PatientSearchResultDto> searchByLastName(String lastName) {
        return patientRepository.findByLastNameContainingIgnoreCase(lastName)
                .stream()
                .map(patientMapper::toSearchResultDto)
                .toList();
    }

    public List<PatientSearchResultDto> searchByFirstName(String firstName) {
        return patientRepository.findByFirstNameContainingIgnoreCase(firstName)
                .stream()
                .map(patientMapper::toSearchResultDto)
                .toList();
    }

    public List<PatientSearchResultDto> searchByFullName(String firstName, String lastName) {
        return patientRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(firstName, lastName)
                .stream()
                .map(patientMapper::toSearchResultDto)
                .toList();
    }

    public Patient getPatientEntityById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }
}
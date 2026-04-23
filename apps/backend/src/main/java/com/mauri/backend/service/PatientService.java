package com.mauri.backend.service;

import com.mauri.backend.dto.patient.PatientDto;
import com.mauri.backend.dto.patient.PatientSearchResultDto;
import com.mauri.backend.dto.patient.UpdatePatientAddressRequest;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.PatientAddress;
import com.mauri.backend.exception.ResourceNotFoundException;
import com.mauri.backend.mapper.PatientMapper;
import com.mauri.backend.repository.PatientAddressRepository;
import com.mauri.backend.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientAddressRepository patientAddressRepository;
    private final PatientMapper patientMapper;

    public PatientService(
            PatientRepository patientRepository,
            PatientAddressRepository patientAddressRepository,
            PatientMapper patientMapper
    ) {
        this.patientRepository = patientRepository;
        this.patientAddressRepository = patientAddressRepository;
        this.patientMapper = patientMapper;
    }

    public PatientDto getPatientById(UUID patientId) {
        return patientMapper.toDto(getPatientEntityById(patientId));
    }

    public List<PatientSearchResultDto> getInitialPatients() {
        return patientRepository.findAllByOrderByPatientNumberAsc()
                .stream()
                .map(patientMapper::toSearchResultDto)
                .toList();
    }

    public List<PatientSearchResultDto> searchByPatientNumber(String patientNumber) {
        return patientRepository.findByPatientNumber(patientNumber)
                .map(patient -> List.of(patientMapper.toSearchResultDto(patient)))
                .orElse(List.of());
    }

    public List<PatientSearchResultDto> searchByBirthDate(LocalDate birthDate) {
        return patientRepository.findByBirthDate(birthDate).stream().map(patientMapper::toSearchResultDto).toList();
    }

    public List<PatientSearchResultDto> searchByLastName(String lastName) {
        return patientRepository.findByLastNameContainingIgnoreCase(lastName).stream().map(patientMapper::toSearchResultDto).toList();
    }

    public List<PatientSearchResultDto> searchByFirstName(String firstName) {
        return patientRepository.findByFirstNameContainingIgnoreCase(firstName).stream().map(patientMapper::toSearchResultDto).toList();
    }

    public List<PatientSearchResultDto> searchByFullName(String firstName, String lastName) {
        return patientRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(firstName, lastName)
                .stream()
                .map(patientMapper::toSearchResultDto)
                .toList();
    }

    public List<PatientSearchResultDto> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String normalizedQuery = query.trim();
        Map<UUID, PatientSearchResultDto> results = new LinkedHashMap<>();

        patientRepository.findByPatientNumber(normalizedQuery)
                .map(patientMapper::toSearchResultDto)
                .ifPresent(dto -> results.put(dto.getId(), dto));

        patientRepository.findTop20ByPatientNumberContainingIgnoreCaseOrderByPatientNumberAsc(normalizedQuery)
                .stream()
                .map(patientMapper::toSearchResultDto)
                .forEach(dto -> results.putIfAbsent(dto.getId(), dto));

        patientRepository.findTop20ByFullNameContainingIgnoreCaseOrderByFullNameAsc(normalizedQuery)
                .stream()
                .map(patientMapper::toSearchResultDto)
                .forEach(dto -> results.putIfAbsent(dto.getId(), dto));

        patientRepository
                .findTop20ByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrderByLastNameAscFirstNameAsc(
                        normalizedQuery,
                        normalizedQuery
                )
                .stream()
                .map(patientMapper::toSearchResultDto)
                .forEach(dto -> results.putIfAbsent(dto.getId(), dto));

        try {
            LocalDate birthDate = LocalDate.parse(normalizedQuery);
            patientRepository.findByBirthDate(birthDate)
                    .stream()
                    .map(patientMapper::toSearchResultDto)
                    .forEach(dto -> results.putIfAbsent(dto.getId(), dto));
        } catch (DateTimeParseException ignored) {
        }

        return results.values().stream().limit(20).toList();
    }

    public PatientDto updatePatientAddress(UUID patientId, UpdatePatientAddressRequest request) {
        Patient patient = getPatientEntityById(patientId);
        PatientAddress address = patientAddressRepository.findByPatient(patient).orElseGet(PatientAddress::new);

        address.setPatient(patient);
        address.setAddressLine(normalize(request.getAddressLine()));
        address.setCity(normalize(request.getCity()));
        address.setState(normalize(request.getState()));
        address.setCounty(normalize(request.getCounty()));
        address.setZipCode(normalize(request.getZipCode()));

        PatientAddress savedAddress = patientAddressRepository.save(address);
        patient.setAddress(savedAddress);

        return patientMapper.toDto(patient);
    }

    public Patient getPatientEntityById(UUID patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

package com.mauri.backend.service;

import com.mauri.backend.dto.prediction.PredictionDto;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.enums.PredictionType;
import com.mauri.backend.mapper.PredictionMapper;
import com.mauri.backend.repository.PredictionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final PatientService patientService;
    private final PredictionMapper predictionMapper;

    public PredictionService(PredictionRepository predictionRepository,
                             PatientService patientService,
                             PredictionMapper predictionMapper) {
        this.predictionRepository = predictionRepository;
        this.patientService = patientService;
        this.predictionMapper = predictionMapper;
    }

    public List<PredictionDto> getPredictionsForPatient(Long patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return predictionRepository.findByPatientOrderByPredictionTimestampDesc(patient)
                .stream()
                .map(predictionMapper::toDto)
                .toList();
    }

    public List<PredictionDto> getPredictionsForPatientByType(Long patientId, PredictionType predictionType) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return predictionRepository.findByPatientAndPredictionTypeOrderByPredictionTimestampDesc(patient, predictionType)
                .stream()
                .map(predictionMapper::toDto)
                .toList();
    }

    public List<PredictionDto> getMainPredictionsForPatient(Long patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return predictionRepository.findByPatientAndMainPredictionTrueOrderByPredictionTimestampDesc(patient)
                .stream()
                .map(predictionMapper::toDto)
                .toList();
    }
}
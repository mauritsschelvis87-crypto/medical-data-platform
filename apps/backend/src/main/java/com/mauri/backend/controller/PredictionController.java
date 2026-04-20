package com.mauri.backend.controller;

import com.mauri.backend.dto.prediction.PredictionDto;
import com.mauri.backend.enums.PredictionType;
import com.mauri.backend.service.PredictionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{patientId}/predictions")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping
    public List<PredictionDto> getPredictionsForPatient(@PathVariable Long patientId) {
        return predictionService.getPredictionsForPatient(patientId);
    }

    @GetMapping("/main")
    public List<PredictionDto> getMainPredictionsForPatient(@PathVariable Long patientId) {
        return predictionService.getMainPredictionsForPatient(patientId);
    }

    @GetMapping("/by-type")
    public List<PredictionDto> getPredictionsForPatientByType(
            @PathVariable Long patientId,
            @RequestParam PredictionType predictionType
    ) {
        return predictionService.getPredictionsForPatientByType(patientId, predictionType);
    }

    @PostMapping
    public PredictionDto savePrediction(
            @PathVariable Long patientId,
            @RequestBody PredictionDto predictionDto
    ) {
        return predictionService.savePrediction(patientId, predictionDto);
    }

    @PutMapping("/{predictionId}/confirm")
    public PredictionDto confirmPrediction(
            @PathVariable Long patientId,
            @PathVariable Long predictionId,
            @RequestParam String doctorName
    ) {
        return predictionService.confirmPrediction(predictionId, doctorName);
    }
}
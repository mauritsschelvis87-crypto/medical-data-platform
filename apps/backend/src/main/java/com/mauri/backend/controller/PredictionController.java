package com.mauri.backend.controller;

import com.mauri.backend.dto.prediction.PredictionDto;
import com.mauri.backend.enums.PredictionType;
import com.mauri.backend.service.PredictionWorkflowService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/patients/{patientId}/predictions")
public class PredictionController {

    private final PredictionService predictionService;
    private final PredictionWorkflowService predictionWorkflowService;

    public PredictionController(PredictionService predictionService,
                                PredictionWorkflowService predictionWorkflowService) {
        this.predictionService = predictionService;
        this.predictionWorkflowService = predictionWorkflowService;
    }

    @GetMapping
    public List<PredictionDto> getPredictionsForPatient(@PathVariable UUID patientId) {
        return predictionService.getPredictionsForPatient(patientId);
    }

    @GetMapping("/main")
    public List<PredictionDto> getMainPredictionsForPatient(@PathVariable UUID patientId) {
        return predictionService.getMainPredictionsForPatient(patientId);
    }

    @GetMapping("/latest")
    public List<PredictionDto> getLatestPredictionsForPatient(@PathVariable UUID patientId) {
        return predictionService.getLatestPredictionsForPatient(patientId);
    }

    @GetMapping("/by-type")
    public List<PredictionDto> getPredictionsForPatientByType(
            @PathVariable UUID patientId,
            @RequestParam PredictionType predictionType
    ) {
        return predictionService.getPredictionsForPatientByType(patientId, predictionType);
    }

    @PostMapping
    public PredictionDto savePrediction(
            @PathVariable UUID patientId,
            @RequestBody PredictionDto predictionDto
    ) {
        return predictionService.savePrediction(patientId, predictionDto);
    }

    @PostMapping("/recalculate")
    public List<PredictionDto> recalculatePredictions(@PathVariable UUID patientId,
                                                      @RequestParam(defaultValue = "MANUAL") String triggerSource) {
        return predictionWorkflowService.recalculatePredictions(patientId, triggerSource, null);
    }

    @PutMapping("/{predictionId}/confirm")
    public PredictionDto confirmPrediction(
            @PathVariable UUID patientId,
            @PathVariable UUID predictionId,
            @RequestParam String doctorName
    ) {
        return predictionService.confirmPrediction(patientId, predictionId, doctorName);
    }
}

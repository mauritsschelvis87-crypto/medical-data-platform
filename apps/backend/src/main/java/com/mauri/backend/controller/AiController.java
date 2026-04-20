package com.mauri.backend.controller;

import com.mauri.backend.dto.prediction.PredictionRequestDto;
import com.mauri.backend.dto.prediction.PredictionResponseDto;
import com.mauri.backend.service.ai.AiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/api/ai/predictions/calculate")
    public PredictionResponseDto calculatePredictions(@RequestBody PredictionRequestDto requestDto) {
        return aiService.calculatePredictions(requestDto);
    }
}
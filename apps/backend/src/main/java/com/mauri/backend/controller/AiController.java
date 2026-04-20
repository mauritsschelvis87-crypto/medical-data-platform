package com.mauri.backend.controller;

import com.mauri.backend.service.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/api/ai/test")
    public String testPrediction() {
        return aiService.getPrediction();
    }
}
package com.mauri.backend.service.ai;

import com.mauri.backend.dto.prediction.PredictionRequestDto;
import com.mauri.backend.dto.prediction.PredictionResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AiService {

    private final WebClient.Builder webClientBuilder;

    public AiService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public PredictionResponseDto calculatePredictions(PredictionRequestDto requestDto) {
        return webClientBuilder
                .build()
                .post()
                .uri("http://localhost:8001/api/v1/predictions/calculate")
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(PredictionResponseDto.class)
                .block();
    }
}
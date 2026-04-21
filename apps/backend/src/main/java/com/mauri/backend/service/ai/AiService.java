package com.mauri.backend.service.ai;

import com.mauri.backend.dto.prediction.PredictionRequestDto;
import com.mauri.backend.dto.prediction.PredictionResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class AiService {

    private final WebClient.Builder webClientBuilder;
    private final String aiBaseUrl;

    public AiService(WebClient.Builder webClientBuilder,
                     @Value("${app.ai.base-url:http://localhost:8001}") String aiBaseUrl) {
        this.webClientBuilder = webClientBuilder;
        this.aiBaseUrl = aiBaseUrl;
    }

    public PredictionResponseDto calculatePredictions(PredictionRequestDto requestDto) {
        try {
            return webClientBuilder
                    .baseUrl(aiBaseUrl)
                    .build()
                    .post()
                    .uri("/api/v1/predictions/calculate")
                    .bodyValue(requestDto)
                    .retrieve()
                    .bodyToMono(PredictionResponseDto.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException("AI service returned an error: " + ex.getStatusCode(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("AI service is unavailable", ex);
        }
    }
}

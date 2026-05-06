package com.mauri.backend.service.ai;

import com.mauri.backend.dto.prediction.PredictionRequestDto;
import com.mauri.backend.dto.prediction.PredictionResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

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
            log.warn("AI service returned {}. Falling back to backend prediction flow.",
                    ex.getStatusCode());
            throw new IllegalStateException("AI service returned an error: " + ex.getStatusCode(), ex);
        } catch (Exception ex) {
            log.warn("AI service is unavailable. Falling back to backend prediction flow.");
            throw new IllegalStateException("AI service is unavailable", ex);
        }
    }
}

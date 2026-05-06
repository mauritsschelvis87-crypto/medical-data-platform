package com.mauri.backend.service.ai;

import com.mauri.backend.dto.prediction.PredictionRequestDto;
import com.mauri.backend.dto.prediction.PredictionResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String PREDICTION_PATH = "/api/v1/predictions/calculate";

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public AiService(RestTemplate restTemplate,
                     @Value("${ai.service.url:http://localhost:8001}") String aiServiceUrl) {
        this.restTemplate = restTemplate;
        this.aiServiceUrl = normalizeBaseUrl(aiServiceUrl);
    }

    public PredictionResponseDto calculatePredictions(PredictionRequestDto requestDto) {
        try {
            return restTemplate.postForObject(
                    aiServiceUrl + PREDICTION_PATH,
                    requestDto,
                    PredictionResponseDto.class
            );
        } catch (HttpStatusCodeException ex) {
            log.error("AI service call failed with status {}. Falling back to backend prediction flow.",
                    ex.getStatusCode());
        } catch (RestClientException ex) {
            log.error("AI service call failed. Falling back to backend prediction flow.", ex);
        }

        return null;
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8001";
        }

        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

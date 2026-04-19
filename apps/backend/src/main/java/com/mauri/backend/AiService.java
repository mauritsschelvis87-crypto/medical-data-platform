package com.mauri.backend;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getPrediction() {
        String url = "http://localhost:8001/predict";
        return restTemplate.getForObject(url, Map.class);
    }
}
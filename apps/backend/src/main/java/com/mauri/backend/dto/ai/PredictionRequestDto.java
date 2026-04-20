package com.mauri.backend.dto.ai;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PredictionRequestDto {

    private Long patientId;
    private String triggerSource;
    private List<String> predictionTypes;
    private Map<String, Object> features;
}
package com.mauri.backend.dto.prediction;

import lombok.Data;

import java.util.List;

@Data
public class PredictionResponseDto {

    private String patientId;
    private String generatedAt;
    private List<PredictionItemDto> predictions;
}

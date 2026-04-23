package com.mauri.backend.dto.prediction;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PredictionResponseDto {

    private UUID patientId;
    private String generatedAt;
    private List<PredictionItemDto> predictions;
}

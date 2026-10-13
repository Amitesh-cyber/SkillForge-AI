package com.studyrec.dto;

import lombok.Data;

@Data
public class RecommendationRequest {
    private String topic;
    private String level;
}

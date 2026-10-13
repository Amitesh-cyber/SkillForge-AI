package com.studyrec.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String topic;
    private String level;
    private String roadmapStep;
    private String mode; // Beginner, Intermediate, Advanced, Interview
}

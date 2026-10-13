package com.studyrec.controller;

import com.studyrec.service.VideoSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/video")
public class VideoSummaryController {

    @Autowired
    private VideoSummaryService videoSummaryService;

    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(@RequestBody Map<String, String> payload) {
        try {
            String videoId = payload.get("videoId");
            String mode = payload.getOrDefault("mode", "default"); // visual, text, default
            String title = payload.getOrDefault("title", "Unknown Video");
            
            String summary = videoSummaryService.summarizeVideo(videoId, mode, title, 1L);
            return ResponseEntity.ok(Map.of("summary", summary));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

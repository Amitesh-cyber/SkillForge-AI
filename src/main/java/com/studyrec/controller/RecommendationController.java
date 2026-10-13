package com.studyrec.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studyrec.dto.RecommendationRequest;
import com.studyrec.model.SavedResource;
import com.studyrec.model.SearchHistory;
import com.studyrec.repository.SavedResourceRepository;
import com.studyrec.repository.SearchHistoryRepository;
import com.studyrec.repository.UserRepository;
import com.studyrec.service.GeminiService;
import com.studyrec.service.YouTubeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // For local development easily
public class RecommendationController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private YouTubeService youtubeService;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private SavedResourceRepository savedResourceRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/recommend")
    public ResponseEntity<Map<String, Object>> recommend(@RequestBody RecommendationRequest request) {
        try {
            // 1. Call Gemini API to get the roadmap structure
            String rawJsonResponse = geminiService.getRecommendations(request.getTopic(), request.getLevel());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawJsonResponse);

            if (root.has("error")) {
                String errorMessage = root.path("message").asText(root.path("error").asText("Unknown AI error"));
                String code = root.path("code").asText();
                if ("SERVICE_UNAVAILABLE".equals(code) || rawJsonResponse.contains("SERVICE_UNAVAILABLE")) {
                    return ResponseEntity.status(503).body(Map.of("error", errorMessage));
                }
                if ("QUOTA_EXCEEDED".equals(code) || rawJsonResponse.contains("QUOTA_EXCEEDED")) {
                    return ResponseEntity.status(429).body(Map.of("error", errorMessage));
                }
                return ResponseEntity.status(500).body(Map.of("error", errorMessage));
            }

            Map<String, Object> roadmapData = mapper.convertValue(root, Map.class);

            // 3. Enrich each step with real YouTube data
            List<Map<String, Object>> steps = (List<Map<String, Object>>) roadmapData.get("steps");
            if (steps != null) {
                for (Map<String, Object> step : steps) {
                    String query = (String) step.get("search_query");
                    if (query != null) {
                        Map<String, Object> playlist = youtubeService.searchBestPlaylist(query);
                        if (playlist != null) {
                            step.put("youtube_resource", playlist);
                        }
                    }
                }
            }

            // 4. Get extra recommended videos
            String extraQuery = (String) roadmapData.get("extra_videos_query");
            if (extraQuery != null) {
                List<Map<String, Object>> extraVideos = youtubeService.searchVideos(extraQuery, 6);
                roadmapData.put("recommended_videos", extraVideos);
            }

            // 5. Save to database (Simplified for now, without user link yet)
            SearchHistory history = new SearchHistory();
            history.setTopic(request.getTopic());
            history.setLevel(request.getLevel());
            history.setResponseJson(mapper.writeValueAsString(roadmapData));
            searchHistoryRepository.save(history);

            roadmapData.put("topic", request.getTopic());
            return ResponseEntity.ok(roadmapData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<SearchHistory>> getHistory() {
        return ResponseEntity.ok(searchHistoryRepository.findAll());
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveResource(@RequestBody SavedResource resource, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            String email = oauthUser.getAttribute("email");
            userRepository.findByEmail(email).ifPresent(resource::setUser);
        } else if (resource.getUser() == null) {
            userRepository.findById(1L).ifPresent(resource::setUser);
        }
        savedResourceRepository.save(resource);
        return ResponseEntity.ok(Map.of("message", "Resource saved"));
    }

    @GetMapping("/saved")
    public ResponseEntity<List<SavedResource>> getSaved() {
        return ResponseEntity.ok(savedResourceRepository.findAll());
    }

    @DeleteMapping("/save/{id}")
    public ResponseEntity<?> unsaveResource(@PathVariable Long id) {
        savedResourceRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Resource removed"));
    }
}

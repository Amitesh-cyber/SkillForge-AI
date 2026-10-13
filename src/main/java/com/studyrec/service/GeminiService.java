package com.studyrec.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public GeminiService() {
        this.restTemplate = new RestTemplate();
    }

    public String getRecommendations(String topic, String level) {
        String prompt = buildPrompt(topic, level);
        return generateContent(prompt, topic, level);
    }

    public String generateContent(String prompt, String topic, String level) {
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String finalUrl = apiUrl + "?key=" + apiKey;
        int attempts = 3;
        while (true) {
            try {
                System.out.println("Gemini request URL: " + finalUrl + " (attempt " + attempts + ")");
                ResponseEntity<Map> response = restTemplate.postForEntity(finalUrl, request, Map.class);
                String text = extractTextFromResponse(response.getBody());
                if (text == null || text.isBlank() || isErrorPayload(text)) {
                    return (topic == null || level == null) ? buildChatFallback() : buildFallbackRoadmap(topic, level);
                }
                return text;
            } catch (RestClientResponseException e) {
                String responseBody = e.getResponseBodyAsString();
                System.err.println("Gemini API Error: " + e.getRawStatusCode() + " " + e.getStatusText() + ": " + responseBody);
                if ((e.getRawStatusCode() == 429 || e.getRawStatusCode() == 503) && attempts > 1) {
                    attempts--;
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    continue;
                }
                if (e.getRawStatusCode() == 429) {
                    return (topic == null || level == null)
                            ? buildChatFallback()
                            : buildErrorJson("QUOTA_EXCEEDED", "AI quota exceeded. Please try again later.");
                }
                if (e.getRawStatusCode() == 503) {
                    return (topic == null || level == null)
                            ? buildChatFallback()
                            : buildFallbackRoadmap(topic, level);
                }
                return (topic == null || level == null)
                        ? buildChatFallback()
                        : buildErrorJson("GENERIC_ERROR", responseBody != null ? responseBody : e.getMessage());
            } catch (Exception e) {
                System.err.println("Gemini API Error: " + e.getMessage());
                if (attempts > 1) {
                    attempts--;
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    continue;
                }
                return (topic == null || level == null)
                        ? buildChatFallback()
                        : buildFallbackRoadmap(topic, level);
            }
        }
    }

    public String generateChatContent(String prompt) {
        String response = generateContent(prompt, null, null);
        if (response == null || response.isBlank() || isErrorPayload(response)) {
            return buildChatFallback();
        }
        return response;
    }


    private String buildPrompt(String topic, String level) {
        return "Act as SkillForge AI, a premium learning mentor. Generate a detailed, step-by-step roadmap and curate high-quality resources for learning " + topic + " at a " + level + " level.\n" +
               "Rules:\n" +
               "* Break down the topic into logical, sequential steps (minimum 5 steps).\n" +
               "* Each step MUST have a clear title, short explanation, and a 'search_query' for finding the best YouTube playlist.\n" +
               "* Suggest trusted channels like freeCodeCamp, Fireship, Traversy Media, etc.\n" +
               "* Also provide a separate list of the BEST websites, FREE courses, and PRACTICE platforms for this topic.\n" +
               "Return JSON ONLY, without markdown blocks:\n" +
               "{\n" +
               "\"roadmap_title\": \"Mastering " + topic + "\",\n" +
               "\"description\": \"A comprehensive path from " + level + " to expert.\",\n" +
               "\"steps\": [\n" +
               "  {\n" +
               "    \"step_number\": 1,\n" +
               "    \"title\": \"Step Title\",\n" +
               "    \"explanation\": \"Brief explanation of what to learn.\",\n" +
               "    \"search_query\": \"best youtube playlist for [Step Title]\",\n" +
               "    \"estimated_time\": \"X hours\"\n" +
               "  }\n" +
               "],\n" +
               "\"resources\": {\n" +
               "  \"websites\": [{\"name\": \"\", \"url\": \"\", \"desc\": \"\"}],\n" +
               "  \"courses\": [{\"name\": \"\", \"url\": \"\", \"platform\": \"\"}],\n" +
               "  \"practice_platforms\": [{\"name\": \"\", \"url\": \"\", \"desc\": \"\"}]\n" +
               "},\n" +
               "\"extra_videos_query\": \"trending tutorials for " + topic + "\"\n" +
               "}";
    }

    private String extractTextFromResponse(Map responseBody) {
        if (responseBody == null || !responseBody.containsKey("candidates")) {
            return null;
        }
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;
            
            Map<String, Object> firstCandidate = candidates.get(0);
            if (firstCandidate == null || !firstCandidate.containsKey("content")) return null;
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            if (content == null || !content.containsKey("parts")) return null;
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            
            if (parts == null || parts.isEmpty()) return null;
            
            String text = (String) parts.get(0).get("text");
            if (text == null) return null;
            text = text.trim();
            // Only strip if it's explicitly a JSON block for the roadmap
            if (text.startsWith("```json")) {
                text = text.substring(7);
                if (text.endsWith("```")) {
                    text = text.substring(0, text.length() - 3);
                }
            }
            return text.trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isErrorPayload(String text) {
        if (text == null) return false;
        String normalized = text.trim();
        if (normalized.startsWith("{") && normalized.contains("\"error\"")) {
            return true;
        }
        return normalized.contains("GENERIC_ERROR") || normalized.contains("QUOTA_EXCEEDED") || normalized.contains("SERVICE_UNAVAILABLE");
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String buildErrorJson(String error, String message) {
        Map<String, String> payload = Map.of(
                "error", error,
                "message", message != null ? message : "Unknown error"
        );
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            return "{\"error\": \"" + error + "\", \"message\": \"" + (message != null ? message.replaceAll("\\\"", "\\\\\"") : "Unknown error") + "\"}";
        }
    }

    private String buildFallbackRoadmap(String topic, String level) {
        Map<String, Object> fallback = Map.of(
                "roadmap_title", "Mastering " + topic + " (Offline Sample)",
                "description", "Gemini is currently overloaded. Here is a starter roadmap based on common learning best practices.",
                "steps", List.of(
                        Map.of(
                                "step_number", 1,
                                "title", "Understand the Basics",
                                "explanation", "Start with the key fundamentals of " + topic + ", including core terminology and the high-level landscape.",
                                "search_query", "best youtube playlist for " + topic + " basics",
                                "estimated_time", "10 hours"
                        ),
                        Map.of(
                                "step_number", 2,
                                "title", "Learn Practical Concepts",
                                "explanation", "Study the essential concepts and workflows that professionals use in " + topic + ".",
                                "search_query", "practical " + topic + " tutorials for beginners",
                                "estimated_time", "15 hours"
                        ),
                        Map.of(
                                "step_number", 3,
                                "title", "Build a Small Project",
                                "explanation", "Apply what you’ve learned by building a small hands-on project related to " + topic + ".",
                                "search_query", "project tutorial for " + topic + " beginner",
                                "estimated_time", "20 hours"
                        )
                ),
                "resources", Map.of(
                        "websites", List.of(
                                Map.of("name", "freeCodeCamp", "url", "https://www.freecodecamp.org/", "desc", "Free tutorials and good beginner resources."),
                                Map.of("name", "YouTube", "url", "https://www.youtube.com/", "desc", "Search for topic-specific playlists and walkthroughs.")
                        ),
                        "courses", List.of(
                                Map.of("name", "Beginner " + topic + " Course", "url", "https://www.coursera.org/", "platform", "Coursera")
                        ),
                        "practice_platforms", List.of(
                                Map.of("name", "Kaggle", "url", "https://www.kaggle.com/", "desc", "Practice with real data and beginner-friendly projects.")
                        )
                ),
                "extra_videos_query", "trending tutorials for " + topic,
                "warning", "This is a local fallback response because the Gemini service is currently overloaded."
        );
        try {
            return OBJECT_MAPPER.writeValueAsString(fallback);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildChatFallback() {
        return "Gemini is currently unavailable. I can’t fetch the AI response right now, but you can ask again in a minute.";
    }
}

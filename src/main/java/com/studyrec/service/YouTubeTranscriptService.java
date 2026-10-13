package com.studyrec.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;

@Service
public class YouTubeTranscriptService {
    
    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchTranscript(String videoId) {
        try {
            String url = "https://www.youtube.com/watch?v=" + videoId;
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            headers.set("Accept-Language", "en-US,en;q=0.9");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);
            String html = response.getBody();
            if (html == null) return "Transcript not available.";

            Pattern pattern = Pattern.compile("\"captions\":(\\{.*?\\}),\"videoDetails\"");
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                String captionsJson = matcher.group(1);
                JSONObject captions = new JSONObject(captionsJson);
                JSONObject renderer = captions.optJSONObject("playerCaptionsTracklistRenderer");
                if (renderer != null) {
                    JSONArray captionTracks = renderer.optJSONArray("captionTracks");
                    if (captionTracks != null && captionTracks.length() > 0) {
                        String transcriptUrl = captionTracks.getJSONObject(0).getString("baseUrl") + "&fmt=json3";
                        
                        org.springframework.http.HttpHeaders reqHeaders = new org.springframework.http.HttpHeaders();
                        reqHeaders.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                        org.springframework.http.HttpEntity<String> reqEntity = new org.springframework.http.HttpEntity<>(reqHeaders);
                        ResponseEntity<String> jsonResponse = restTemplate.exchange(transcriptUrl, org.springframework.http.HttpMethod.GET, reqEntity, String.class);
                        
                        String jsonBody = jsonResponse.getBody();
                        if (jsonBody != null && !jsonBody.isEmpty()) {
                            JSONObject transcriptData = new JSONObject(jsonBody);
                            JSONArray events = transcriptData.optJSONArray("events");
                            if (events != null) {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < events.length(); i++) {
                                    JSONObject event = events.getJSONObject(i);
                                    JSONArray segs = event.optJSONArray("segs");
                                    if (segs != null) {
                                        for (int j = 0; j < segs.length(); j++) {
                                            String text = segs.getJSONObject(j).optString("utf8", "");
                                            sb.append(text).append(" ");
                                        }
                                    }
                                }
                                return sb.toString().replaceAll("\\s+", " ").trim();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Transcript fetch error: " + e.getMessage());
        }
        return "Could not fetch transcript. The video might not have public captions.";
    }
}

package com.studyrec.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class YouTubeService {

    @Value("${youtube.api.key}")
    private String apiKey;

    @Value("${youtube.application.name}")
    private String applicationName;

    private YouTube youtubeService;

    public YouTubeService() {
        // Initialize lazily or in @PostConstruct
    }

    private YouTube getService() throws GeneralSecurityException, IOException {
        if (youtubeService == null) {
            youtubeService = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    null)
                    .setApplicationName(applicationName)
                    .build();
        }
        return youtubeService;
    }

    public Map<String, Object> searchBestPlaylist(String query) {
        try {
            YouTube youtube = getService();
            YouTube.Search.List request = youtube.search().list(List.of("snippet"));
            request.setKey(apiKey);
            request.setQ(query);
            request.setType(List.of("playlist"));
            request.setMaxResults(1L);
            
            SearchListResponse response = request.execute();
            List<SearchResult> results = response.getItems();
            
            if (results != null && !results.isEmpty()) {
                SearchResult result = results.get(0);
                Map<String, Object> data = new HashMap<>();
                data.put("title", result.getSnippet().getTitle());
                data.put("playlistId", result.getId().getPlaylistId());
                data.put("thumbnail", result.getSnippet().getThumbnails().getHigh().getUrl());
                data.put("channelTitle", result.getSnippet().getChannelTitle());
                data.put("url", "https://www.youtube.com/playlist?list=" + result.getId().getPlaylistId());
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Map<String, Object>> searchVideos(String query, int maxResults) {
        List<Map<String, Object>> videos = new ArrayList<>();
        try {
            YouTube youtube = getService();
            YouTube.Search.List request = youtube.search().list(List.of("snippet"));
            request.setKey(apiKey);
            request.setQ(query);
            request.setType(List.of("video"));
            request.setMaxResults((long) maxResults);
            
            SearchListResponse response = request.execute();
            List<SearchResult> results = response.getItems();
            
            if (results != null) {
                for (SearchResult result : results) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", result.getSnippet().getTitle());
                    data.put("videoId", result.getId().getVideoId());
                    data.put("thumbnail", result.getSnippet().getThumbnails().getHigh().getUrl());
                    data.put("channelTitle", result.getSnippet().getChannelTitle());
                    data.put("url", "https://www.youtube.com/watch?v=" + result.getId().getVideoId());
                    videos.add(data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videos;
    }
}

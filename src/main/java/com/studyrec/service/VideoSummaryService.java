package com.studyrec.service;

import com.studyrec.model.VideoSummary;
import com.studyrec.repository.VideoSummaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VideoSummaryService {

    @Autowired
    private YouTubeTranscriptService transcriptService;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private VideoSummaryRepository repository;

    public String summarizeVideo(String videoId, String mode, String title, Long userId) {
        // Optional: Check cache in DB if needed.

        String transcript = transcriptService.fetchTranscript(videoId);
        boolean hasTranscript = true;
        if (transcript == null || transcript.startsWith("Could not fetch") || transcript.trim().isEmpty()) {
            hasTranscript = false;
            transcript = "";
        }
        
        // Truncate transcript to save tokens if it's too long (e.g. 30000 chars)
        if (hasTranscript && transcript.length() > 30000) {
            transcript = transcript.substring(0, 30000);
        }

        String prompt = buildPrompt(transcript, mode, title, hasTranscript);
        String summary = geminiService.generateChatContent(prompt);
        
        return summary;
    }

    private String buildPrompt(String transcript, String mode, String title, boolean hasTranscript) {
        String baseContext = hasTranscript 
            ? "Analyze the following video transcript for the video titled '" + title + "':\n" + transcript + "\n\n"
            : "I could not fetch the exact transcript for the video titled '" + title + "'. Based on your extensive knowledge about this topic, please explain the concepts that would likely be covered in this video.\n\n";

        if ("visual".equalsIgnoreCase(mode)) {
            return baseContext + "Create a strictly visual explanation using markdown. " +
                   "Use flowchart-like explanations, step-by-step diagrams, visual hierarchy, and concept trees. " +
                   "Do not use mermaid, just use text-based ASCII arrows or structured markdown lists (e.g., A \u2192 B \u2192 C).";
        } else if ("text".equalsIgnoreCase(mode)) {
            return baseContext + "Convert the topic of this video into structured, highly readable learning notes. " +
                   "Include chapter-wise summaries and revision notes. Format clearly with markdown.";
        } else {
            return baseContext + "Provide a highly structured and comprehensive summary of this topic. " +
                   "Include EXACTLY these sections:\n" +
                   "1. Quick Summary\n" +
                   "2. Key Concepts\n" +
                   "3. Important Points\n" +
                   "4. Practical Examples\n" +
                   "5. Real-world Use Cases\n" +
                   "6. Interview Questions\n" +
                   "7. Beginner Explanation\n" +
                   "8. Diagram-style explanation (using text/arrows)\n" +
                   "9. Important commands/code snippets (if applicable)\n" +
                   "Return the output in clean Markdown.";
        }
    }
}

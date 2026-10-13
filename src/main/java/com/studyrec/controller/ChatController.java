package com.studyrec.controller;

import com.studyrec.dto.ChatRequest;
import com.studyrec.model.ChatHistory;
import com.studyrec.repository.ChatHistoryRepository;
import com.studyrec.repository.UserRepository;
import com.studyrec.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        try {
            String prompt = buildChatPrompt(request);
            String response = geminiService.generateChatContent(prompt);
            if (response == null || response.isBlank()) {
                return ResponseEntity.status(503).body(Map.of("error", "AI service unavailable. Please try again later."));
            }

            // Save to history
            ChatHistory history = new ChatHistory();
            userRepository.findById(1L).ifPresent(history::setUser); 
            history.setMessage(request.getMessage());
            history.setResponse(response);
            history.setTopic(request.getTopic());
            chatHistoryRepository.save(history);
            
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private String buildChatPrompt(ChatRequest request) {
        return "Act as SkillForge Assistant, an elite AI Study Mentor and Personal Tutor.\n" +
               "Context:\n" +
               "- Current Topic: " + request.getTopic() + "\n" +
               "- User Level: " + request.getLevel() + "\n" +
               "- Current Roadmap Step: " + request.getRoadmapStep() + "\n" +
               "- Explanation Mode: " + request.getMode() + "\n\n" +
               "User's Question: " + request.getMessage() + "\n\n" +
               "Rules:\n" +
               "1. Use clear, structured formatting (Markdown).\n" +
               "2. Provide real-world analogies if possible.\n" +
               "3. If explaining code, use syntax highlighting.\n" +
               "4. If the mode is 'Interview', focus on common questions and how to answer them.\n" +
               "5. Suggest 2-3 follow-up questions at the end.";
    }
}

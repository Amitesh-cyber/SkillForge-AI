package com.studyrec.controller;

import com.studyrec.model.*;
import com.studyrec.repository.*;
import com.studyrec.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private CompilerService compilerService;

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private CodingSubmissionRepository codingSubmissionRepository;

    @Autowired
    private InterviewFeedbackRepository feedbackRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/session/start")
    public ResponseEntity<?> startSession(@RequestBody Map<String, Object> request) {
        try {
            String mode = (String) request.get("mode");
            String company = (String) request.get("company");
            Map<String, Object> metadata = (Map<String, Object>) request.getOrDefault("metadata", new HashMap<>());

            if (mode == null || company == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mode and Company are required."));
            }

            User user = userRepository.findById(1L).orElse(null);
            if (user == null) {
                user = new User();
                user.setName("Default Learner");
                user.setEmail("learner@skillforge.ai");
                user.setPassword("123456");
                user = userRepository.save(user);
            }

            InterviewSession session = interviewService.startSession(user.getId(), mode, company, metadata);
            
            return ResponseEntity.ok(Map.of(
                "sessionId", session.getId(),
                "title", session.getTitle(),
                "mode", session.getMode(),
                "company", session.getCompany(),
                "status", session.getStatus(),
                "transcript", session.getTranscript(),
                "createdAt", session.getCreatedAt()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/session/{id}/chat")
    public ResponseEntity<?> chat(@PathVariable("id") Long sessionId, @RequestBody Map<String, String> body) {
        try {
            String userMessage = body.get("message");
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty."));
            }

            String aiResponse = interviewService.processInterviewMessage(sessionId, userMessage);
            return ResponseEntity.ok(Map.of("response", aiResponse));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/session/{id}/run-code")
    public ResponseEntity<?> runCode(@PathVariable("id") Long sessionId, @RequestBody Map<String, String> body) {
        try {
            String code = body.get("code");
            String language = body.get("language");
            String stdin = body.getOrDefault("stdin", "");

            if (code == null || language == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Code and language are required."));
            }

            Map<String, Object> result = compilerService.executeCode(code, language, stdin);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/session/{id}/submit-code")
    public ResponseEntity<?> submitCode(@PathVariable("id") Long sessionId, @RequestBody Map<String, String> body) {
        try {
            String code = body.get("code");
            String language = body.get("language");
            String problemTitle = body.getOrDefault("problemTitle", "Coding Challenge");

            if (code == null || language == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Code and language are required."));
            }

            InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
            if (session == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Session not found"));
            }

            Map<String, Object> compileResult = compilerService.executeCode(code, language, "");
            String stderr = (String) compileResult.getOrDefault("stderr", "");
            int exitCode = Integer.parseInt(compileResult.getOrDefault("exitCode", 0).toString());

            CodingSubmission sub = new CodingSubmission();
            sub.setInterviewSession(session);
            sub.setCode(code);
            sub.setLanguage(language);
            sub.setProblemTitle(problemTitle);
            sub.setStatus((exitCode == 0 && stderr.isEmpty()) ? "PASSED" : "FAILED");
            sub.setOutput((String) compileResult.getOrDefault("stdout", ""));
            codingSubmissionRepository.save(sub);

            return ResponseEntity.ok(Map.of(
                "status", sub.getStatus(),
                "output", sub.getOutput(),
                "stderr", stderr,
                "compilerMessage", compileResult.getOrDefault("compilerMessage", "")
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/session/{id}/complete")
    public ResponseEntity<?> completeSession(@PathVariable("id") Long sessionId) {
        try {
            InterviewFeedback feedback = interviewService.completeSessionAndAnalyze(sessionId);
            if (feedback == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Session not found"));
            }
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/aptitude/questions")
    public ResponseEntity<?> getAptitudeQuestions() {
        try {
            List<Map<String, Object>> questions = interviewService.generateAptitudeQuestions();
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        try {
            List<InterviewSession> sessions = sessionRepository.findByUserId(1L);
            List<Map<String, Object>> summary = new ArrayList<>();
            for (InterviewSession s : sessions) {
                summary.add(Map.of(
                    "id", s.getId(),
                    "title", s.getTitle(),
                    "mode", s.getMode(),
                    "company", s.getCompany(),
                    "status", s.getStatus(),
                    "score", s.getScore() != null ? s.getScore() : 0.0,
                    "createdAt", s.getCreatedAt().toString()
                ));
            }
            Collections.reverse(summary);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/session/{id}")
    public ResponseEntity<?> getSessionDetail(@PathVariable("id") Long sessionId) {
        try {
            InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
            if (session == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Session not found"));
            }

            Optional<InterviewFeedback> feedback = feedbackRepository.findByInterviewSessionId(sessionId);
            List<CodingSubmission> submissions = codingSubmissionRepository.findByInterviewSessionId(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("session", session);
            response.put("feedback", feedback.orElse(null));
            response.put("submissions", submissions);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

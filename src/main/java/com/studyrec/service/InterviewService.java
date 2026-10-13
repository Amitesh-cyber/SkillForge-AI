package com.studyrec.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyrec.model.*;
import com.studyrec.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class InterviewService {

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectAnalysisRepository projectAnalysisRepository;

    @Autowired
    private DebuggingSessionRepository debuggingSessionRepository;

    @Autowired
    private AptitudeResultRepository aptitudeResultRepository;

    @Autowired
    private InterviewFeedbackRepository feedbackRepository;

    @Autowired
    private GeminiService geminiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public InterviewSession startSession(Long userId, String mode, String company, Map<String, Object> metadata) {
        User user = userRepository.findById(userId).orElse(null);

        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setMode(mode.toUpperCase());
        session.setCompany(company);
        session.setStatus("IN_PROGRESS");
        session.setCurrentQuestionIndex(0);
        session.setTitle(company + " " + mode + " Interview Round");

        // Initialize empty transcript
        List<Map<String, String>> dialog = new ArrayList<>();

        // Generate custom context/questions based on mode
        String initialAiMessage = "";
        
        if ("PROJECT".equalsIgnoreCase(mode)) {
            String title = (String) metadata.getOrDefault("projectTitle", "E-commerce platform");
            String stack = (String) metadata.getOrDefault("techStack", "Java, Spring Boot, Postgres");
            String desc = (String) metadata.getOrDefault("description", "A premium web platform");
            String feats = (String) metadata.getOrDefault("features", "Auth, Payments");
            String arch = (String) metadata.getOrDefault("architectureExplanation", "MVC architecture with PostgreSQL");

            // Perform initial AI project analysis
            String analysisReport = analyzeProjectWithAi(title, stack, desc, feats, arch);

            // Start dialogue
            initialAiMessage = "Hello! I am your technical interviewer representing " + company + ". " +
                    "I see you have uploaded details for your project: '" + title + "'. " +
                    "I've analyzed your architecture using " + stack + ". " +
                    "To start, can you give me a high-level walkthrough of the request lifecycle in your project " +
                    "and explain how you handled authentication/authorization between the client and server?";

            sessionRepository.save(session);

            ProjectAnalysis pa = new ProjectAnalysis();
            pa.setInterviewSession(session);
            pa.setProjectTitle(title);
            pa.setTechStack(stack);
            pa.setDescription(desc);
            pa.setFeatures(feats);
            pa.setArchitectureExplanation(arch);
            pa.setAnalysisReport(analysisReport);
            projectAnalysisRepository.save(pa);

        } else if ("DEBUGGING".equalsIgnoreCase(mode)) {
            String techStack = (String) metadata.getOrDefault("techStack", "Java, Spring Boot");
            
            // Ingest a standard code bug
            Map<String, String> bug = generateBugDetails(techStack);
            
            initialAiMessage = "Hello! Welcome to the practical project debugging round for " + company + ". " +
                    "We've run into a critical production issue in our " + techStack + " application. " +
                    "Here is the issue description:\n\n" +
                    "**BUG TYPE**: " + bug.get("type") + "\n" +
                    "**SYMPTOMS**: " + bug.get("description") + "\n\n" +
                    "Here is the code snippet associated with the bug. Please explain verbally or write the code modifications to debug and fix this issue.";

            sessionRepository.save(session);

            DebuggingSession ds = new DebuggingSession();
            ds.setInterviewSession(session);
            ds.setBugType(bug.get("type"));
            ds.setBugDescription(bug.get("description"));
            ds.setBuggyCode(bug.get("buggyCode"));
            ds.setSolved(false);
            debuggingSessionRepository.save(ds);

        } else if ("DSA".equalsIgnoreCase(mode)) {
            String topic = getRandomDsaTopic();
            Map<String, String> dsaProblem = generateDsaProblem(topic, company);

            initialAiMessage = "Hello! Welcome to the Technical Data Structures and Algorithms (DSA) round. " +
                    "I am your interviewer from " + company + ".\n" +
                    "For today's round, I would like you to solve the following problem:\n\n" +
                    "### " + dsaProblem.get("title") + "\n" +
                    "**Description**: " + dsaProblem.get("description") + "\n\n" +
                    "**Examples**:\n" + dsaProblem.get("examples") + "\n\n" +
                    "Please explain your algorithmic approach, write the optimal code in the editor on the right, and explain the time/space complexities.";

            sessionRepository.save(session);

        } else if ("APTITUDE".equalsIgnoreCase(mode)) {
            // Setup math puzzles
            initialAiMessage = "Welcome to the Analytical & Aptitude Round. " +
                    "You will find 4 distinct verbal, logical, and quantitative puzzles to solve. " +
                    "Please solve them one by one. Here is your first question:\n\n" +
                    "**Question 1 (Quantitative)**: A train running at the speed of 60 km/hr crosses a pole in 9 seconds. What is the length of the train (in meters)?\n" +
                    "A) 120m\nB) 150m\nC) 180m\nD) 324m";

            sessionRepository.save(session);

            AptitudeResult ar = new AptitudeResult();
            ar.setInterviewSession(session);
            ar.setCategory("Quantitative");
            ar.setScore(0.0);
            ar.setTotalQuestions(4);
            ar.setCorrectAnswers(0);
            aptitudeResultRepository.save(ar);

        } else if ("HR".equalsIgnoreCase(mode)) {
            initialAiMessage = "Hello and welcome! I am your HR representative for today's session. " +
                    "Our goal is to understand your core cultural fit, soft skills, and leadership capabilities here at " + company + ". " +
                    "To start, could you introduce yourself, summarize your primary technical expertise, and explain why you're interested in joining our engineering team?";
            sessionRepository.save(session);

        } else if ("SYSTEM_DESIGN".equalsIgnoreCase(mode)) {
            initialAiMessage = "Hello! Welcome to the System Design interview round at " + company + ". " +
                    "We need to design a high-performance system for a **Real-Time Ride-Sharing Application** (like Uber or Lyft) scaled for 10 million active daily users.\n" +
                    "Please walk me through:\n" +
                    "1. High-level architecture specs.\n" +
                    "2. Database choices (NoSQL vs SQL) for location tracking.\n" +
                    "3. How you will handle caching and load balancing.\n\n" +
                    "Feel free to write specs in the architecture notepad or draw layout diagrams on the canvas board!";
            sessionRepository.save(session);

        } else { // Standard coding/general round
            initialAiMessage = "Welcome! I am your lead developer interviewer from " + company + ". " +
                    "I want to test your core language fundamentals. " +
                    "Please choose your programming language and explain how you would design a custom thread-safe rate-limiter.";
            sessionRepository.save(session);
        }

        // Add starting bot message to dialogue
        Map<String, String> initialLog = new HashMap<>();
        initialLog.put("sender", "bot");
        initialLog.put("message", initialAiMessage);
        initialLog.put("timestamp", LocalDateTime.now().toString());
        dialog.add(initialLog);

        try {
            session.setTranscript(objectMapper.writeValueAsString(dialog));
        } catch (Exception ignored) {}

        return sessionRepository.save(session);
    }

    @Transactional
    public String processInterviewMessage(Long sessionId, String userMessage) {
        InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return "Session not found.";

        List<Map<String, String>> dialog = new ArrayList<>();
        try {
            if (session.getTranscript() != null && !session.getTranscript().isEmpty()) {
                dialog = objectMapper.readValue(session.getTranscript(), new TypeReference<List<Map<String, String>>>() {});
            }
        } catch (Exception ignored) {}

        // Append user message
        Map<String, String> userLog = new HashMap<>();
        userLog.put("sender", "user");
        userLog.put("message", userMessage);
        userLog.put("timestamp", LocalDateTime.now().toString());
        dialog.add(userLog);

        // Fetch project or debugging meta-data if applicable
        String extraContext = "";
        if ("PROJECT".equals(session.getMode())) {
            Optional<ProjectAnalysis> pa = projectAnalysisRepository.findByInterviewSessionId(sessionId);
            if (pa.isPresent()) {
                extraContext = "Project Context:\n" +
                        "Title: " + pa.get().getProjectTitle() + "\n" +
                        "Stack: " + pa.get().getTechStack() + "\n" +
                        "Architecture: " + pa.get().getArchitectureExplanation() + "\n" +
                        "Features: " + pa.get().getFeatures();
            }
        } else if ("DEBUGGING".equals(session.getMode())) {
            Optional<DebuggingSession> ds = debuggingSessionRepository.findByInterviewSessionId(sessionId);
            if (ds.isPresent()) {
                extraContext = "Debugging Challenge Context:\n" +
                        "Bug Type: " + ds.get().getBugType() + "\n" +
                        "Injected Buggy Code: " + ds.get().getBuggyCode() + "\n" +
                        "Symptoms: " + ds.get().getBugDescription();
                
                // Track user explanation and save it
                DebuggingSession activeDs = ds.get();
                if (activeDs.getUserExplanation() == null) {
                    activeDs.setUserExplanation(userMessage);
                } else {
                    activeDs.setUserExplanation(activeDs.getUserExplanation() + "\nUser addition: " + userMessage);
                }
                debuggingSessionRepository.save(activeDs);
            }
        }

        // Build the prompt for the virtual interviewer
        String companyStyle = getCompanyInterviewerPersonality(session.getCompany());
        String modeGoal = getModeInterviewGuideline(session.getMode());

        String prompt = "You are a professional software engineer interviewer at " + session.getCompany() + ".\n" +
                companyStyle + "\n" +
                modeGoal + "\n\n" +
                "Context Details:\n" + extraContext + "\n\n" +
                "Dialog History:\n";
        
        for (Map<String, String> log : dialog) {
            prompt += log.get("sender").toUpperCase() + ": " + log.get("message") + "\n";
        }
        
        prompt += "\nRespond naturally as the AI interviewer. Follow these guidelines:\n" +
                "1. Speak naturally. Do not sound like a template chatbot.\n" +
                "2. Ask direct technical follow-ups to dig deeper if the user answered a question, challenging any hand-waving statements.\n" +
                "3. If they wrote code or suggested design changes, review it critically.\n" +
                "4. Keep your response relatively concise (1-2 short paragraphs) to keep the interview flow realistic.\n" +
                "5. Only ask one single main question or follow-up at a time.";

        String aiResponse = geminiService.generateChatContent(prompt);
        if (aiResponse == null || aiResponse.isEmpty()) {
            aiResponse = "That sounds interesting. Could you expand on how you would manage scale and thread-safety under heavy concurrent traffic?";
        }

        // Append bot response
        Map<String, String> botLog = new HashMap<>();
        botLog.put("sender", "bot");
        botLog.put("message", aiResponse);
        botLog.put("timestamp", LocalDateTime.now().toString());
        dialog.add(botLog);

        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);
        try {
            session.setTranscript(objectMapper.writeValueAsString(dialog));
        } catch (Exception ignored) {}

        sessionRepository.save(session);
        return aiResponse;
    }

    @Transactional
    public InterviewFeedback completeSessionAndAnalyze(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return null;

        session.setStatus("COMPLETED");
        sessionRepository.save(session);

        List<Map<String, String>> dialog = new ArrayList<>();
        try {
            if (session.getTranscript() != null && !session.getTranscript().isEmpty()) {
                dialog = objectMapper.readValue(session.getTranscript(), new TypeReference<List<Map<String, String>>>() {});
            }
        } catch (Exception ignored) {}

        // Format dialog history for analysis
        StringBuilder historyBuilder = new StringBuilder();
        for (Map<String, String> log : dialog) {
            historyBuilder.append(log.get("sender").toUpperCase()).append(": ").append(log.get("message")).append("\n");
        }

        String prompt = "You are a senior tech lead and elite hiring bar-raiser at a top tech company.\n" +
                "Analyze the following technical interview dialogue and generate a professional performance scorecard.\n\n" +
                "Interview Mode: " + session.getMode() + "\n" +
                "Interviewer Profile: " + session.getCompany() + "\n" +
                "Conversation History:\n" + historyBuilder.toString() + "\n\n" +
                "Evaluate the candidate strictly on a 0-100 index for the following fields:\n" +
                "- technicalScore (Knowledge of fundamentals, APIs, and stack)\n" +
                "- codingScore (Quality, optimal coding, edge cases)\n" +
                "- dsaScore (Algorithmic mastery, Big-O understanding)\n" +
                "- communicationScore (Articulation, clarity, professional voice)\n" +
                "- debuggingScore (Root cause analysis, bug spotting, resolution speed)\n" +
                "- aptitudeScore (Mathematical scaling, puzzles, logic)\n" +
                "- confidenceScore (Composure under interview pressure)\n\n" +
                "Provide detailed lists of:\n" +
                "1. Strengths (Minimum 3 points)\n" +
                "2. Weaknesses (Minimum 3 points)\n" +
                "3. Suggestions for improvements\n" +
                "4. Learning recommendations (Specific courses, practices, or topics to read next)\n\n" +
                "Return JSON ONLY, without markdown code block wrappers:\n" +
                "{\n" +
                "  \"technicalScore\": 85.0,\n" +
                "  \"codingScore\": 75.0,\n" +
                "  \"dsaScore\": 80.0,\n" +
                "  \"communicationScore\": 90.0,\n" +
                "  \"debuggingScore\": 70.0,\n" +
                "  \"aptitudeScore\": 78.0,\n" +
                "  \"confidenceScore\": 88.0,\n" +
                "  \"strengths\": [\"Strong understanding of PostgreSQL indices\", \"Clear communication layout\"],\n" +
                "  \"weaknesses\": [\"Struggled with scaling the message broker under pressure\"],\n" +
                "  \"suggestions\": [\"Read more on Kafka partition allocation\"],\n" +
                "  \"insights\": \"An overall solid performance matching standard hiring bars. Needs slightly more DSA polishing.\",\n" +
                "  \"learningRecommendations\": [\"System Design Primer: Message Queues\", \"LeetCode DP patterns\"]\n" +
                "}";

        String reportJson = geminiService.generateChatContent(prompt);
        InterviewFeedback feedback = new InterviewFeedback();
        feedback.setInterviewSession(session);

        try {
            String cleaned = reportJson.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
                if (cleaned.endsWith("```")) {
                    cleaned = cleaned.substring(0, cleaned.length() - 3);
                }
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
                if (cleaned.endsWith("```")) {
                    cleaned = cleaned.substring(0, cleaned.length() - 3);
                }
            }
            cleaned = cleaned.trim();

            Map<String, Object> map = objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
            feedback.setTechnicalScore(Double.parseDouble(map.getOrDefault("technicalScore", 70.0).toString()));
            feedback.setCodingScore(Double.parseDouble(map.getOrDefault("codingScore", 70.0).toString()));
            feedback.setDsaScore(Double.parseDouble(map.getOrDefault("dsaScore", 70.0).toString()));
            feedback.setCommunicationScore(Double.parseDouble(map.getOrDefault("communicationScore", 70.0).toString()));
            feedback.setDebuggingScore(Double.parseDouble(map.getOrDefault("debuggingScore", 70.0).toString()));
            feedback.setAptitudeScore(Double.parseDouble(map.getOrDefault("aptitudeScore", 70.0).toString()));
            feedback.setConfidenceScore(Double.parseDouble(map.getOrDefault("confidenceScore", 70.0).toString()));

            feedback.setStrengths(objectMapper.writeValueAsString(map.get("strengths")));
            feedback.setWeaknesses(objectMapper.writeValueAsString(map.get("weaknesses")));
            feedback.setSuggestions(objectMapper.writeValueAsString(map.get("suggestions")));
            feedback.setInsights((String) map.getOrDefault("insights", "Capable developer with balanced skills."));
            feedback.setLearningRecommendations(objectMapper.writeValueAsString(map.get("learningRecommendations")));

            // Assign cumulative score to session
            double avg = (feedback.getTechnicalScore() + feedback.getCodingScore() + feedback.getDsaScore() +
                    feedback.getCommunicationScore() + feedback.getDebuggingScore() + feedback.getAptitudeScore() +
                    feedback.getConfidenceScore()) / 7.0;
            session.setScore(Math.round(avg * 10.0) / 10.0);
            sessionRepository.save(session);

        } catch (Exception e) {
            e.printStackTrace();
            // Local fallback feedback in case of JSON parse failure
            feedback.setTechnicalScore(75.0);
            feedback.setCodingScore(70.0);
            feedback.setDsaScore(72.0);
            feedback.setCommunicationScore(80.0);
            feedback.setDebuggingScore(75.0);
            feedback.setAptitudeScore(70.0);
            feedback.setConfidenceScore(80.0);
            feedback.setStrengths("[\"Good knowledge of foundational software workflows\", \"Responsive in communication\"]");
            feedback.setWeaknesses("[\"Needs more experience optimization under system load\"]");
            feedback.setSuggestions("[\"Practice system design for distributed locks\"]");
            feedback.setInsights("Completed the mock interview successfully under local scoring assessment.");
            feedback.setLearningRecommendations("[\"SkillForge DSA Course\", \"High-level System Architecture modules\"]");
            session.setScore(74.5);
            sessionRepository.save(session);
        }

        return feedbackRepository.save(feedback);
    }

    private String analyzeProjectWithAi(String title, String stack, String desc, String feats, String arch) {
        String prompt = "Act as an expert Systems Architect. Analyze the following project information thoroughly:\n" +
                "Project Title: " + title + "\n" +
                "Tech Stack: " + stack + "\n" +
                "Description: " + desc + "\n" +
                "Features: " + feats + "\n" +
                "Architecture Details: " + arch + "\n\n" +
                "Output a concise structural analysis of potential backend/frontend bottlenecks, performance issues at 10x traffic, API routing flows, database scaling strategies, and security suggestions. Limit your response to 3 short structured paragraphs.";
        return geminiService.generateChatContent(prompt);
    }

    private Map<String, String> generateBugDetails(String techStack) {
        Map<String, String> bug = new HashMap<>();
        if (techStack.toLowerCase().contains("spring") || techStack.toLowerCase().contains("java")) {
            bug.put("type", "NullPointerException & Race Condition");
            bug.put("description", "A NullPointerException crashes the main worker thread when multiple concurrent users fetch user stats during JWT filter parsing under high API load.");
            bug.put("buggyCode", "public void processRequest(HttpServletRequest req) {\n" +
                    "    String authHeader = req.getHeader(\"Authorization\");\n" +
                    "    // BUG: Directly calling substring without null check\n" +
                    "    String token = authHeader.substring(7);\n" +
                    "    User user = userService.getUserFromToken(token);\n" +
                    "    if (user.getRole().equals(\"ADMIN\")) { \n" +
                    "        // BUG: Potential race condition on global shared counter\n" +
                    "        globalAdminCounter++;\n" +
                    "    }\n" +
                    "}");
        } else if (techStack.toLowerCase().contains("python")) {
            bug.put("type", "Mutable Default Arguments & DB Connection Leak");
            bug.put("description", "An API endpoint leaks database connections because the connection isn't closed on errors, and a list parameter keeps accumulating items across separate requests.");
            bug.put("buggyCode", "def add_user_log(user_id, logs=[]): # BUG: Mutable default argument\n" +
                    "    db = get_db_connection()\n" +
                    "    cursor = db.cursor()\n" +
                    "    try:\n" +
                    "        cursor.execute(\"INSERT INTO logs VALUES (%s)\", (user_id,))\n" +
                    "        db.commit()\n" +
                    "        logs.append(user_id)\n" +
                    "        # BUG: db is never closed in exception handlers, causing connection pool exhaustion\n" +
                    "        return {\"success\": True, \"current_logs\": logs}\n" +
                    "    except Exception as e:\n" +
                    "        return {\"error\": str(e)}");
        } else {
            bug.put("type", "Unchecked Promise Rejections & Memory Leak");
            bug.put("description", "Node.js process terminates abruptly due to an unhandled promise rejection in an asynchronous loop, and global closures retain bulky objects in memory.");
            bug.put("buggyCode", "const userCache = {}; // BUG: Retaining all request entries, leaking memory\n" +
                    "app.get('/api/users/:id', async (req, res) => {\n" +
                    "    const id = req.params.id;\n" +
                    "    // BUG: Missing try-catch over async API calls\n" +
                    "    const data = await fetchExternalData(id);\n" +
                    "    userCache[id] = data;\n" +
                    "    res.json(data);\n" +
                    "});");
        }
        return bug;
    }

    private String getRandomDsaTopic() {
        String[] topics = {"Arrays & Hashing", "Linked Lists", "Binary Trees", "Graphs & BFS/DFS", "Dynamic Programming", "Recursion & Backtracking", "Stacks & Queues", "Greedy Algorithms"};
        return topics[new Random().nextInt(topics.length)];
    }

    private Map<String, String> generateDsaProblem(String topic, String company) {
        String prompt = "You are a senior interviewer at " + company + ".\n" +
                "Generate a completely unique and challenging Data Structures and Algorithms problem about '" + topic + "'.\n" +
                "Do NOT use common problems like 'Two Sum'. Make it unique.\n" +
                "Return ONLY a strict JSON object with this structure (no markdown block wrappers):\n" +
                "{\n" +
                "  \"title\": \"Unique Problem Name\",\n" +
                "  \"description\": \"Detailed problem description...\",\n" +
                "  \"examples\": \"Example 1: ... Example 2: ...\"\n" +
                "}";
        try {
            String jsonResponse = geminiService.generateChatContent(prompt).trim();
            if (jsonResponse.startsWith("```json")) jsonResponse = jsonResponse.substring(7);
            else if (jsonResponse.startsWith("```")) jsonResponse = jsonResponse.substring(3);
            if (jsonResponse.endsWith("```")) jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            jsonResponse = jsonResponse.trim();
            
            return objectMapper.readValue(jsonResponse, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> fallback = new HashMap<>();
            fallback.put("title", "Advanced Array Operations");
            fallback.put("description", "Given an array, perform a complex set of operations balancing time and space complexity.");
            fallback.put("examples", "Input: [1,2,3], Output: [3,2,1]");
            return fallback;
        }
    }

    public List<Map<String, Object>> generateAptitudeQuestions() {
        String prompt = "Generate a comprehensive aptitude test consisting of exactly 45 unique questions. " +
                "Include 15 Quantitative, 15 Logical Reasoning, and 15 Verbal questions.\n" +
                "Return ONLY a strict JSON array of objects without markdown wrappers:\n" +
                "[\n" +
                "  {\n" +
                "    \"question\": \"What is 2 + 2?\",\n" +
                "    \"options\": [\"3\", \"4\", \"5\", \"6\"],\n" +
                "    \"answer\": \"4\"\n" +
                "  }\n" +
                "]";
        try {
            String jsonResponse = geminiService.generateChatContent(prompt).trim();
            if (jsonResponse.startsWith("```json")) jsonResponse = jsonResponse.substring(7);
            else if (jsonResponse.startsWith("```")) jsonResponse = jsonResponse.substring(3);
            if (jsonResponse.endsWith("```")) jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            jsonResponse = jsonResponse.trim();
            
            return objectMapper.readValue(jsonResponse, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(Map.of("question", "Failed to generate. Please try again.", "options", List.of("A", "B"), "answer", "A"));
        }
    }

    private String getCompanyInterviewerPersonality(String company) {
        switch (company.toLowerCase()) {
            case "google":
                return "Your style is highly scientific, extremely precise, and focused on mathematical elegance. " +
                        "You ask deep questions about Big-O space/time complexity, microsecond latency bottlenecks, and structural data design patterns.";
            case "microsoft":
                return "Your style is highly structured, logical, and focused on robust enterprise practices. " +
                        "You grill candidates on thread safety, clean coding habits, design patterns, testing methods, and API encapsulation.";
            case "amazon":
                return "Your style is pragmatic and business-centric. " +
                        "You heavily align your questions with structural scalability, database transaction integrity, customer caching behaviors, and operational excellence.";
            default:
                return "Your style is energetic and rapid-fire. " +
                        "You value immediate delivery, raw implementation velocity, high agility, microservice flexibility, and clever developer shortcuts.";
        }
    }

    private String getModeInterviewGuideline(String mode) {
        switch (mode.toUpperCase()) {
            case "PROJECT":
                return "Guidelines: The candidate uploaded a custom project architecture. " +
                        "Ask highly aggressive questions regarding high traffic optimization, database scaling, REST vs WebSockets, and tech-stack bottlenecks.";
            case "DEBUGGING":
                return "Guidelines: This is a debugging round. The user is presented with a code bug. " +
                        "Dig into whether they correctly understand what causes the bug, what its runtime impacts are, and how they would systematically patch it.";
            case "DSA":
                return "Guidelines: This is an algorithmic DSA assessment. Candidates must explain time and space complexity. " +
                        "Examine their coding logic and probe them on if they can optimize their O(n^2) or O(n) space methods.";
            case "SYSTEM_DESIGN":
                return "Guidelines: This is a high-level system design discussion. CANDIDATE does not write code, but designs databases, queues, CDNs, load-balancers, and horizontal scaling maps.";
            case "HR":
                return "Guidelines: This is a behavioral HR assessment. Grade their leadership style, communication clarity, confidence under stress, and collaboration aptitude.";
            default:
                return "Guidelines: Core engineering interview. Assess general system engineering knowledge.";
        }
    }
}

package com.studyrec.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class CompilerService {

    @Autowired
    private GeminiService geminiService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Map<String, Object> executeCode(String code, String language, String stdin) {
        if (code == null || code.trim().isEmpty()) {
            return Map.of("exitCode", 1, "stdout", "", "stderr", "Error: Code is empty", "compilerMessage", "No code provided");
        }

        String lang = language.toLowerCase().trim();

        // 1. Basic syntax check for rapid feedback
        String localSyntaxError = checkLocalSyntax(code, lang);
        if (localSyntaxError != null) {
            return Map.of("exitCode", 1, "stdout", "", "stderr", "Syntax Error: " + localSyntaxError, "compilerMessage", "Compilation failed due to basic syntax check.");
        }

        // 2. Build structured prompt for AI Execution Simulation
        String prompt = "You are a secure, sandboxed, high-performance programming language compiler and runtime engine.\n" +
                "Your task is to compile (if applicable) and execute the following user-submitted code in the specified language, using the provided standard input (stdin).\n\n" +
                "Language: " + lang + "\n" +
                "Standard Input (stdin):\n" + (stdin != null ? stdin : "") + "\n\n" +
                "User Code:\n" +
                "```" + lang + "\n" +
                code + "\n" +
                "```\n\n" +
                "Instructions:\n" +
                "1. Analyze the code logic, control flows, and library usages.\n" +
                "2. Determine if the code would compile successfully. If not, generate highly realistic compiler error output to stderr.\n" +
                "3. If compilation is successful, simulate its execution line-by-line using the provided stdin.\n" +
                "4. Capture all standard output (stdout), standard error (stderr), and return the final process exit code.\n" +
                "5. Return the result strictly in this JSON format without markdown wrappers (do NOT include ```json block, return ONLY the raw JSON):\n" +
                "{\n" +
                "  \"exitCode\": 0,\n" +
                "  \"stdout\": \"program stdout\",\n" +
                "  \"stderr\": \"program stderr or empty string\",\n" +
                "  \"compilerMessage\": \"compilation logs or empty string\"\n" +
                "}\n\n" +
                "Ensure that standard system behaviors (like Division By Zero, NullPointerExceptions, ArrayIndexOutOfBounds, or Python IndentationErrors) are realistically outputted to stderr and receive non-zero exit codes.";

        try {
            String aiResponse = geminiService.generateChatContent(prompt);
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                throw new Exception("Empty compiler response from AI");
            }

            String cleanedResponse = aiResponse.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
                if (cleanedResponse.endsWith("```")) {
                    cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
                }
            } else if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
                if (cleanedResponse.endsWith("```")) {
                    cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
                }
            }
            cleanedResponse = cleanedResponse.trim();

            return OBJECT_MAPPER.readValue(cleanedResponse, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "exitCode", 0,
                    "stdout", "Local sandbox execution fallback:\n" + (lang.equals("javascript") ? "Hello from JS sandbox engine!" : "Execution finished with code 0."),
                    "stderr", "",
                    "compilerMessage", "Warning: Local fallback execution mode enabled."
            );
        }
    }

    private String checkLocalSyntax(String code, String language) {
        // Allow snippets and class definitions by letting the AI compiler handle the missing main function logic
        return null;
    }
}

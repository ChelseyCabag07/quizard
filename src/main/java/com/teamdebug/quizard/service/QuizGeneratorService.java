package com.teamdebug.quizard.service;

import com.teamdebug.quizard.model.dto.QuizQuestion;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuizGeneratorService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<QuizQuestion> generateQuiz(String text, int numberOfQuestions, String difficulty) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }
        
        if (numberOfQuestions < 1 || numberOfQuestions > 50) {
            throw new IllegalArgumentException("Number of questions must be between 1 and 50");
        }
        
        System.out.println("Generating quiz with " + numberOfQuestions + " questions, difficulty: " + difficulty);
        
        String prompt = buildQuizPrompt(text, numberOfQuestions, difficulty);
        String response = callAIService(prompt);
        
        return parseQuizResponse(response, numberOfQuestions);
    }
    
    private String buildQuizPrompt(String text, int numberOfQuestions, String difficulty) {
        return String.format("""
            Create exactly %d multiple-choice quiz questions based on the following text.
            Difficulty level: %s
            
            Requirements:
            - Each question must have exactly 4 options labeled A, B, C, D
            - Only ONE correct answer per question
            - Include a brief explanation for each correct answer
            - Questions should test understanding, not just memorization
            - Vary question types (definition, application, comparison, analysis)
            - Make questions clear and unambiguous
            
            IMPORTANT: Return ONLY a valid JSON array with this exact structure. Do not include any markdown formatting, code blocks, or extra text:
            
            [
              {
                "question": "Question text here?",
                "options": ["A) First option", "B) Second option", "C) Third option", "D) Fourth option"],
                "correctAnswer": "A",
                "explanation": "Brief explanation why this is correct"
              }
            ]
            
            Text to generate questions from:
            
            %s
            
            Generate exactly %d questions as a JSON array.
            """, numberOfQuestions, difficulty, text, numberOfQuestions);
    }
    
    private String callAIService(String prompt) {
        try {
            System.out.println("Calling OpenAI API...");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a quiz generator. You must always respond with valid JSON array only. Never use markdown code blocks or any formatting. Just pure JSON."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 3000);
            requestBody.put("temperature", 0.7);
            
            // Make API call using RestTemplate
            String response = restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions",
                requestBody,
                String.class
            );
            
            String content = extractContentFromResponse(response);
            System.out.println("AI Response received, length: " + content.length());
            
            return content;
            
        } catch (Exception e) {
            System.err.println("Error calling AI service: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error generating quiz: " + e.getMessage());
        }
    }
    
    private String extractContentFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0)
                      .path("message").path("content").asText();
        } catch (Exception e) {
            System.err.println("Error extracting content from response: " + e.getMessage());
            throw new RuntimeException("Error parsing AI response");
        }
    }
    
    private List<QuizQuestion> parseQuizResponse(String jsonResponse, int expectedCount) {
        try {
            System.out.println("Parsing quiz response...");
            System.out.println("Raw response: " + jsonResponse.substring(0, Math.min(200, jsonResponse.length())));
            
            // Remove markdown code blocks if present
            String cleanJson = jsonResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
            
            // If it starts with text before JSON, try to extract just the JSON array
            int jsonStart = cleanJson.indexOf('[');
            int jsonEnd = cleanJson.lastIndexOf(']');
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                cleanJson = cleanJson.substring(jsonStart, jsonEnd + 1);
            }
            
            System.out.println("Clean JSON: " + cleanJson.substring(0, Math.min(200, cleanJson.length())));
            
            // Parse JSON array into list of QuizQuestion objects
            List<QuizQuestion> questions = objectMapper.readValue(
                cleanJson, 
                new TypeReference<List<QuizQuestion>>() {}
            );
            
            System.out.println("Successfully parsed " + questions.size() + " questions");
            
            // Validate questions
            for (int i = 0; i < questions.size(); i++) {
                QuizQuestion q = questions.get(i);
                if (q.getQuestion() == null || q.getQuestion().isEmpty()) {
                    throw new RuntimeException("Question " + i + " is missing question text");
                }
                if (q.getOptions() == null || q.getOptions().size() != 4) {
                    throw new RuntimeException("Question " + i + " must have exactly 4 options");
                }
                if (q.getCorrectAnswer() == null || q.getCorrectAnswer().isEmpty()) {
                    throw new RuntimeException("Question " + i + " is missing correct answer");
                }
            }
            
            return questions;
            
        } catch (Exception e) {
            System.err.println("Error parsing quiz response: " + e.getMessage());
            e.printStackTrace();
            // Return fallback questions on error
            return createFallbackQuestions(expectedCount);
        }
    }
    
    private List<QuizQuestion> createFallbackQuestions(int count) {
        System.out.println("Creating fallback questions due to parsing error");
        List<QuizQuestion> fallback = new ArrayList<>();
        
        for (int i = 1; i <= Math.min(count, 3); i++) {
            fallback.add(new QuizQuestion(
                "multiple_choice",
                "Sample Question " + i + " - Unable to generate from AI",
                List.of(
                    "A) Option 1", 
                    "B) Option 2", 
                    "C) Option 3", 
                    "D) Option 4"
                ),
                "A",
                "This is a fallback question. Please check your API key and try again."
            ));
        }
        
        return fallback;
    }
}
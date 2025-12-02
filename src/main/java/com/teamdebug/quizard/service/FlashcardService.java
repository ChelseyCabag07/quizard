package com.teamdebug.quizard.service;

import com.teamdebug.quizard.model.dto.Flashcard;
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
public class FlashcardService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<Flashcard> generateFlashcards(String text, int numberOfCards) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }
        
        if (numberOfCards < 1 || numberOfCards > 50) {
            throw new IllegalArgumentException("Number of flashcards must be between 1 and 50");
        }
        
        System.out.println("Generating " + numberOfCards + " flashcards");
        
        String prompt = buildFlashcardPrompt(text, numberOfCards);
        String response = callAIService(prompt);
        
        return parseFlashcardResponse(response, numberOfCards);
    }
    
    private String buildFlashcardPrompt(String text, int numberOfCards) {
        return String.format("""
            Create exactly %d flashcards based on the following text.
            
            Requirements:
            - Front: A clear, concise question or term (keep it short, under 100 characters)
            - Back: A comprehensive answer or definition (can be 1-3 sentences)
            - Cover the most important concepts from the text
            - Vary types: definitions, explanations, comparisons, applications, examples
            - Make them useful for studying and memorization
            - Each flashcard should focus on ONE key concept
            
            IMPORTANT: Return ONLY a valid JSON array with this exact structure. Do not include any markdown formatting, code blocks, or extra text:
            
            [
              {
                "front": "What is OOP?",
                "back": "Object-Oriented Programming is a programming paradigm that organizes code around objects containing data and methods."
              }
            ]
            
            Text to generate flashcards from:
            
            %s
            
            Generate exactly %d flashcards as a JSON array.
            """, numberOfCards, text, numberOfCards);
    }
    
    private String callAIService(String prompt) {
        try {
            System.out.println("Calling OpenAI API for flashcards...");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a flashcard generator. You must always respond with valid JSON array only. Never use markdown code blocks or any formatting. Just pure JSON."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 2500);
            requestBody.put("temperature", 0.7);
            
            // Make API call using RestTemplate
            String response = restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions",
                requestBody,
                String.class
            );
            
            String content = extractContentFromResponse(response);
            System.out.println("AI Response received for flashcards, length: " + content.length());
            
            return content;
            
        } catch (Exception e) {
            System.err.println("Error calling AI service: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error generating flashcards: " + e.getMessage());
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
    
    private List<Flashcard> parseFlashcardResponse(String jsonResponse, int expectedCount) {
        try {
            System.out.println("Parsing flashcard response...");
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
            
            // Parse JSON array into list of Flashcard objects
            List<Flashcard> flashcards = objectMapper.readValue(
                cleanJson, 
                new TypeReference<List<Flashcard>>() {}
            );
            
            System.out.println("Successfully parsed " + flashcards.size() + " flashcards");
            
            // Validate flashcards
            for (int i = 0; i < flashcards.size(); i++) {
                Flashcard card = flashcards.get(i);
                if (card.getFront() == null || card.getFront().isEmpty()) {
                    throw new RuntimeException("Flashcard " + i + " is missing front text");
                }
                if (card.getBack() == null || card.getBack().isEmpty()) {
                    throw new RuntimeException("Flashcard " + i + " is missing back text");
                }
            }
            
            return flashcards;
            
        } catch (Exception e) {
            System.err.println("Error parsing flashcard response: " + e.getMessage());
            e.printStackTrace();
            // Return fallback flashcards on error
            return createFallbackFlashcards(expectedCount);
        }
    }
    
    private List<Flashcard> createFallbackFlashcards(int count) {
        System.out.println("Creating fallback flashcards due to parsing error");
        List<Flashcard> fallback = new ArrayList<>();
        
        for (int i = 1; i <= Math.min(count, 3); i++) {
            fallback.add(new Flashcard(
                "Error Card " + i,
                "Unable to generate flashcards from AI. Please check your API key and try again."
            ));
        }
        
        return fallback;
    }
}
package com.teamdebug.quizard.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

    
    @Service
public class SummaryService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public String summarize(String text) {
        if (text.isEmpty()) {
            return "No text provided to summarize.";
        }
        
        // Summarize all text comprehensively in one pass if possible
        return createDetailedSummary(text);
    }
    
    private String createDetailedSummary(String text) {
        String prompt = buildDetailedPrompt(text);
        return callAIService(prompt);
    }
    
    private String buildDetailedPrompt(String text) {
        return """
            Create a comprehensive and detailed summary of the following text.
            
            Requirements:
            - Include ALL major concepts and key points
            - Provide explanations for technical terms
            - Keep important examples and comparisons
            - Organize into clear sections with headings
            - Aim for 30-40% of original length (NOT just bullet points)
            - Use paragraphs, not just short bullet points
            - Maintain the educational value of the content
            
            Text to summarize:
            
            """ + text + """
            
            
            Provide a detailed summary:
            """;
    }
    
    private String callAIService(String prompt) {
        // Example for OpenAI API
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo"); // or gpt-4
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 2000); // Increase for longer summaries
            requestBody.put("temperature", 0.3); // Lower = more focused
            
            // Make API call
            String response = restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions",
                createHttpEntity(requestBody),
                String.class
            );
            
            // Parse and return summary
            return extractSummaryFromResponse(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating summary: " + e.getMessage();
        }
    }
    
    private Object createHttpEntity(Map<String, Object> body) {
        // This would need HttpHeaders and HttpEntity imports
        // For now, return the body directly
        return body;
    }
    
    private String extractSummaryFromResponse(String response) {
        // Parse JSON response and extract the summary text
        // This depends on your AI service response format
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            return root.path("choices").get(0)
                      .path("message").path("content").asText();
        } catch (Exception e) {
            return "Error parsing response";
        }
    }
}
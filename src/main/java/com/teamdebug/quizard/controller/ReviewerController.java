package com.teamdebug.quizard.controller;

import com.teamdebug.quizard.model.dto.ReviewerResponse;
import com.teamdebug.quizard.model.entity.Flashcard;
import com.teamdebug.quizard.model.entity.QuizItem;
import com.teamdebug.quizard.model.entity.Reviewer;
import com.teamdebug.quizard.service.FileExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/reviewers")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class ReviewerController {

    @Autowired
    private FileExtractionService fileExtractionService;

    // In-memory storage (replace with database later)
    private Map<Long, Reviewer> reviewers = new HashMap<>();
    private Map<Long, List<Flashcard>> flashcardsMap = new HashMap<>();
    private Map<Long, List<QuizItem>> quizItemsMap = new HashMap<>();
    private Long nextId = 1L;
    private Long nextFlashcardId = 1L;
    private Long nextQuizItemId = 1L;

    // Upload file endpoint
    @PostMapping("/upload")
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Read file content using FileExtractionService
            String fileName = file.getOriginalFilename();
            String content = fileExtractionService.extractText(file); // ✅ FIXED - Now extracts properly!

            // Create reviewer
            Reviewer reviewer = new Reviewer();
            reviewer.setId(nextId);
            reviewer.setFileName(fileName);
            reviewer.setOriginalText(content);
            reviewer.setSummarizedText(generateSummary(content));

            // Store reviewer
            reviewers.put(nextId, reviewer);

            // Generate flashcards and quiz items
            List<Flashcard> flashcards = generateFlashcards(content, nextId);
            List<QuizItem> quizItems = generateQuizItems(content, nextId);

            flashcardsMap.put(nextId, flashcards);
            quizItemsMap.put(nextId, quizItems);

            response.put("id", nextId);
            response.put("fileName", fileName);
            response.put("message", "File uploaded successfully");

            nextId++;

        } catch (Exception e) {
            response.put("error", "Upload failed: " + e.getMessage());
        }

        return response;
    }

    // Get summary endpoint
    @GetMapping("/{id}/summary")
    public Map<String, String> getSummary(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();

        Reviewer reviewer = reviewers.get(id);
        if (reviewer != null) {
            response.put("summary", reviewer.getSummarizedText());
        } else {
            response.put("error", "Reviewer not found");
        }

        return response;
    }

    // Generate flashcards endpoint
    @PostMapping("/{id}/flashcards")
    public List<Flashcard> getFlashcards(@PathVariable Long id) {
        return flashcardsMap.getOrDefault(id, new ArrayList<>());
    }

    // Generate quiz endpoint
    @PostMapping("/{id}/quiz")
    public List<QuizItem> getQuiz(@PathVariable Long id) {
        return quizItemsMap.getOrDefault(id, new ArrayList<>());
    }

    // Get complete reviewer response
    @GetMapping("/{id}")
    public ReviewerResponse getReviewer(@PathVariable Long id) {
        Reviewer reviewer = reviewers.get(id);
        if (reviewer == null) {
            return null;
        }

        return new ReviewerResponse(
                reviewer.getId(),
                reviewer.getSummarizedText(),
                flashcardsMap.getOrDefault(id, new ArrayList<>()),
                quizItemsMap.getOrDefault(id, new ArrayList<>())
        );
    }

    // Helper: Generate summary
    private String generateSummary(String content) {
        String[] sentences = content.split("[.!?]+");
        StringBuilder summary = new StringBuilder("📋 KEY POINTS:\n\n");

        int count = 0;
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > 20 && count < 5) {
                summary.append(++count).append(". ").append(trimmed).append(".\n\n");
            }
        }

        if (count == 0) {
            summary.append("Summary could not be generated. Content may be too short.");
        }

        return summary.toString();
    }

    // Helper: Generate flashcards
    private List<Flashcard> generateFlashcards(String content, Long reviewerId) {
        List<Flashcard> flashcards = new ArrayList<>();
        String[] sentences = content.split("[.!?]+");

        for (int i = 0; i < Math.min(10, sentences.length); i++) {
            String sentence = sentences[i].trim();
            if (sentence.length() > 30) {
                // Create term (first part of sentence)
                String[] words = sentence.split(" ");
                String term = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(5, words.length)));

                // Create definition (full sentence)
                String definition = sentence;

                Flashcard flashcard = new Flashcard();
                flashcard.setId(nextFlashcardId++);
                flashcard.setTerm("Q: " + term + "...");
                flashcard.setDefinition(definition);

                flashcards.add(flashcard);
            }
        }

        return flashcards;
    }

    // Helper: Generate quiz items
    private List<QuizItem> generateQuizItems(String content, Long reviewerId) {
        List<QuizItem> quizItems = new ArrayList<>();
        String[] sentences = content.split("[.!?]+");

        for (int i = 0; i < Math.min(5, sentences.length); i++) {
            String sentence = sentences[i].trim();
            if (sentence.length() > 30) {
                // Create multiple choice question
                QuizItem quizItem = new QuizItem();
                quizItem.setId(nextQuizItemId++);
                quizItem.setQuestion("What is the main idea of this statement?");
                
                // Create choices (correct answer + distractors)
                List<String> choices = new ArrayList<>();
                choices.add(sentence); // Correct answer
                choices.add("This is an incorrect option");
                choices.add("This is also incorrect");
                choices.add("Not the correct answer");
                
                // Shuffle choices
                Collections.shuffle(choices);
                
                quizItem.setChoices(choices);
                quizItem.setCorrectAnswer(sentence);
                quizItem.setType("MCQ");

                quizItems.add(quizItem);
            }
        }

        return quizItems;
    }
}
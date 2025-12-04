package com.teamdebug.quizard.controller;

import com.teamdebug.quizard.model.dto.ReviewerResponse;
import com.teamdebug.quizard.model.entity.Flashcard;
import com.teamdebug.quizard.model.entity.QuizItem;
import com.teamdebug.quizard.model.entity.Reviewer;
import com.teamdebug.quizard.service.FileExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.bind.annotation.RequestMethod;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviewers")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
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
            String content = fileExtractionService.extractText(file);

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
            if (trimmed.length() > 10 && count < 10) {  // Lowered from 20 to 10
                summary.append(++count).append(". ").append(trimmed).append(".\n\n");
            }
        }

        if (count == 0) {
            summary.append("Summary: ").append(content.substring(0, Math.min(200, content.length())));
        }

        return summary.toString();
    }

    // Helper: Generate flashcards
    private List<Flashcard> generateFlashcards(String content, Long reviewerId) {
        List<Flashcard> flashcards = new ArrayList<>();
        String[] sentences = content.split("[.!?]+");

        for (int i = 0; i < Math.min(10, sentences.length); i++) {
            String sentence = sentences[i].trim();
            if (sentence.length() > 10) {  // Lowered from 30 to 10
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

    // Helper: Generate quiz items with content-based questions
    private List<QuizItem> generateQuizItems(String content, Long reviewerId) {
        List<QuizItem> quizItems = new ArrayList<>();
        String[] sentences = content.split("[.!?]+");
        
        // Filter valid sentences
        List<String> validSentences = new ArrayList<>();
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > 15) {
                validSentences.add(trimmed);
            }
        }
        
        if (validSentences.isEmpty()) {
            return quizItems;
        }

        // Generate different types of questions
        for (int i = 0; i < Math.min(5, validSentences.size()); i++) {
            String sentence = validSentences.get(i);
            String[] words = sentence.split("\\s+");
            
            QuizItem quizItem = new QuizItem();
            quizItem.setId(nextQuizItemId++);
            quizItem.setType("MCQ");
            
            // Create fill-in-the-blank style question
            if (words.length >= 5) {
                // Pick a key word to blank out (not first or last word)
                int blankIndex = Math.min(3, words.length / 2);
                String blankWord = words[blankIndex];
                
                // Create question with blank
                StringBuilder questionBuilder = new StringBuilder("Complete the sentence: ");
                for (int j = 0; j < words.length; j++) {
                    if (j == blankIndex) {
                        questionBuilder.append("________ ");
                    } else {
                        questionBuilder.append(words[j]).append(" ");
                    }
                }
                quizItem.setQuestion(questionBuilder.toString().trim());
                
                // Create choices from content
                List<String> choices = new ArrayList<>();
                choices.add(blankWord); // Correct answer
                
                // Get distractors from other sentences
                Set<String> usedWords = new HashSet<>();
                usedWords.add(blankWord.toLowerCase());
                
                for (String otherSentence : validSentences) {
                    if (choices.size() >= 4) break;
                    String[] otherWords = otherSentence.split("\\s+");
                    for (String word : otherWords) {
                        if (word.length() > 3 && !usedWords.contains(word.toLowerCase()) && choices.size() < 4) {
                            choices.add(word);
                            usedWords.add(word.toLowerCase());
                        }
                    }
                }
                
                // Fill remaining with generic if needed
                while (choices.size() < 4) {
                    choices.add("Option " + (choices.size() + 1));
                }
                
                Collections.shuffle(choices);
                quizItem.setChoices(choices);
                quizItem.setCorrectAnswer(blankWord);
            } else {
                // Fallback to true/false style
                quizItem.setQuestion("Is this statement from the document? \"" + sentence + "\"");
                List<String> choices = new ArrayList<>(Arrays.asList("True - This is correct", "False - This is incorrect", "Partially correct", "Cannot determine"));
                quizItem.setChoices(choices);
                quizItem.setCorrectAnswer("True - This is correct");
            }

            quizItems.add(quizItem);
        }

        return quizItems;
    }
}
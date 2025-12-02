package com.teamdebug.quizard.model.dto;

import java.util.List;

public class FlashcardResponse {
    private List<Flashcard> flashcards;
    private String message;
    
    public FlashcardResponse() {}
    
    public FlashcardResponse(List<Flashcard> flashcards) {
        this.flashcards = flashcards;
    }
    
    public FlashcardResponse(String message) {
        this.message = message;
    }
    
    public List<Flashcard> getFlashcards() {
        return flashcards;
    }
    
    public void setFlashcards(List<Flashcard> flashcards) {
        this.flashcards = flashcards;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
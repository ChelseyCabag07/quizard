package com.teamdebug.quizard.model.dto;

public class FlashcardRequest {
    private String text;
    private int numberOfCards;
    
    public FlashcardRequest() {}
    
    public FlashcardRequest(String text, int numberOfCards) {
        this.text = text;
        this.numberOfCards = numberOfCards;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public int getNumberOfCards() {
        return numberOfCards;
    }
    
    public void setNumberOfCards(int numberOfCards) {
        this.numberOfCards = numberOfCards;
    }
}
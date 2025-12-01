package com.teamdebug.quizard.model.dto;

import com.teamdebug.quizard.model.entity.Flashcard;
import com.teamdebug.quizard.model.entity.QuizItem;
import java.util.List;

public class ReviewerResponse {

    private Long reviewerId;
    private String summarizedText;
    private List<Flashcard> flashcards;
    private List<QuizItem> quizItems;

    public ReviewerResponse() {}

    public ReviewerResponse(Long reviewerId, String summarizedText,
                            List<Flashcard> flashcards, List<QuizItem> quizItems) {
        this.reviewerId = reviewerId;
        this.summarizedText = summarizedText;
        this.flashcards = flashcards;
        this.quizItems = quizItems;
    }

    public Long getReviewerId() { return reviewerId; }
    public String getSummarizedText() { return summarizedText; }
    public List<Flashcard> getFlashcards() { return flashcards; }
    public List<QuizItem> getQuizItems() { return quizItems; }

    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public void setSummarizedText(String summarizedText) { this.summarizedText = summarizedText; }
    public void setFlashcards(List<Flashcard> flashcards) { this.flashcards = flashcards; }
    public void setQuizItems(List<QuizItem> quizItems) { this.quizItems = quizItems; }
    
}

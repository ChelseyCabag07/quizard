package com.teamdebug.quizard.model.dto;

import com.teamdebug.quizard.model.entity.QuizItem;
import java.util.List;

public class QuizResponse {

     private Long reviewerId;
    private List<QuizItem> questions;

    public QuizResponse() {}

    public QuizResponse(Long reviewerId, List<QuizItem> questions) {
        this.reviewerId = reviewerId;
        this.questions = questions;
    }

    public Long getReviewerId() { return reviewerId; }
    public List<QuizItem> getQuestions() { return questions; }

    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public void setQuestions(List<QuizItem> questions) { this.questions = questions; }
    
}

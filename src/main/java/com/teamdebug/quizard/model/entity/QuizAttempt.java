package com.teamdebug.quizard.model.entity;

import java.util.Map;

public class QuizAttempt {

     private Long id;
    private Long reviewerId;
    private Map<Long, String> userAnswers; // QuizItem.id → user answer
    private int score;

    public QuizAttempt() {}

    public QuizAttempt(Long id, Long reviewerId, Map<Long, String> userAnswers, int score) {
        this.id = id;
        this.reviewerId = reviewerId;
        this.userAnswers = userAnswers;
        this.score = score;
    }

    public Long getId() { return id; }
    public Long getReviewerId() { return reviewerId; }
    public Map<Long, String> getUserAnswers() { return userAnswers; }
    public int getScore() { return score; }

    public void setId(Long id) { this.id = id; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public void setUserAnswers(Map<Long, String> userAnswers) { this.userAnswers = userAnswers; }
    public void setScore(int score) { this.score = score; }

    
}

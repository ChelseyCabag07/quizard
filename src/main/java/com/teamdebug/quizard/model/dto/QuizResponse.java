package com.teamdebug.quizard.model.dto;

import java.util.List;

public class QuizResponse {
    private List<QuizQuestion> questions;
    private String message;
    
    public QuizResponse() {}
    
    public QuizResponse(List<QuizQuestion> questions) {
        this.questions = questions;
    }
    
    public QuizResponse(String message) {
        this.message = message;
    }
    
    public List<QuizQuestion> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<QuizQuestion> questions) {
        this.questions = questions;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

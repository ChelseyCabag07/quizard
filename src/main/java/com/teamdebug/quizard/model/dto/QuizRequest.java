package com.teamdebug.quizard.model.dto;

public class QuizRequest {
    private String text;
    private int numberOfQuestions;
    private String difficulty;
    private String quizType;
    
    public QuizRequest() {}
    
    public QuizRequest(String text, int numberOfQuestions, String difficulty, String quizType) {
        this.text = text;
        this.numberOfQuestions = numberOfQuestions;
        this.difficulty = difficulty;
        this.quizType = quizType;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public int getNumberOfQuestions() {
        return numberOfQuestions;
    }
    
    public void setNumberOfQuestions(int numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getQuizType() {
        return quizType;
    }
    
    public void setQuizType(String quizType) {
        this.quizType = quizType;
    }
}
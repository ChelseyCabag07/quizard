package com.teamdebug.quizard.model.dto;

import java.util.List;

public class QuizQuestion {
    private String type;
    private String question;
    private List<String> options;
    private String correctAnswer;
    private List<String> correctAnswers;
    private String explanation;
    private Integer points;
    
    public QuizQuestion() {}
    
    public QuizQuestion(String type, String question, List<String> options, 
                       String correctAnswer, String explanation) {
        this.type = type;
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }
    
    public QuizQuestion(String type, String question, String correctAnswer, String explanation) {
        this.type = type;
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }
    
    public QuizQuestion(String type, String question, List<String> correctAnswers, 
                       String explanation, Integer points) {
        this.type = type;
        this.question = question;
        this.correctAnswers = correctAnswers;
        this.explanation = explanation;
        this.points = points;
    }
    
    // All Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public List<String> getOptions() {
        return options;
    }
    
    public void setOptions(List<String> options) {
        this.options = options;
    }
    
    public String getCorrectAnswer() {
        return correctAnswer;
    }
    
    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
    
    public List<String> getCorrectAnswers() {
        return correctAnswers;
    }
    
    public void setCorrectAnswers(List<String> correctAnswers) {
        this.correctAnswers = correctAnswers;
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
    
    public Integer getPoints() {
        return points;
    }
    
    public void setPoints(Integer points) {
        this.points = points;
    }
}
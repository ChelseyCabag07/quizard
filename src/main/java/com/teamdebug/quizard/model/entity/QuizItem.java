package com.teamdebug.quizard.model.entity;

import java.util.List;

public class QuizItem {

     private Long id;
    private String question;
    private List<String> choices;
    private String correctAnswer;
    private String type; // MCQ, IDENTIFICATION, ENUMERATION

    public QuizItem() {}

    public QuizItem(Long id, String question, List<String> choices,
                    String correctAnswer, String type) {
        this.id = id;
        this.question = question;
        this.choices = choices;
        this.correctAnswer = correctAnswer;
        this.type = type;
    }

    public Long getId() { return id; }
    public String getQuestion() { return question; }
    public List<String> getChoices() { return choices; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getType() { return type; }

    public void setId(Long id) { this.id = id; }
    public void setQuestion(String question) { this.question = question; }
    public void setChoices(List<String> choices) { this.choices = choices; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public void setType(String type) { this.type = type; }
}


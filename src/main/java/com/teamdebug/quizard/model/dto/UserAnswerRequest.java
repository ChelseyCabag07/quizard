package com.teamdebug.quizard.model.dto;

import java.util.Map;

public class UserAnswerRequest {

    private Map<Long, String> answers;
    public UserAnswerRequest() {}

    public UserAnswerRequest(Map<Long, String> answers) {
        this.answers = answers;
    }

    public Map<Long, String> getAnswers() { return answers; }
    public void setAnswers(Map<Long, String> answers) { this.answers = answers; }
    
}

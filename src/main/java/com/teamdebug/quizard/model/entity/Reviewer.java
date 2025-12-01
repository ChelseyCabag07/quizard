package com.teamdebug.quizard.model.entity;

public class Reviewer {

    private Long id;
    private String fileName;
    private String originalText;
    private String summarizedText;

    public Reviewer() {}

    public Reviewer(Long id, String fileName, String originalText, String summarizedText) {
        this.id = id;
        this.fileName = fileName;
        this.originalText = originalText;
        this.summarizedText = summarizedText;
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getOriginalText() { return originalText; }
    public String getSummarizedText() { return summarizedText; }

    public void setId(Long id) { this.id = id; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
    public void setSummarizedText(String summarizedText) { this.summarizedText = summarizedText; }
}

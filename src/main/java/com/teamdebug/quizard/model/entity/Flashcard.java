package com.teamdebug.quizard.model.entity;

public class Flashcard {

     private Long id;
    private String term;
    private String definition;

    public Flashcard() {}

    public Flashcard(Long id, String term, String definition) {
        this.id = id;
        this.term = term;
        this.definition = definition;
    }

    public Long getId() { return id; }
    public String getTerm() { return term; }
    public String getDefinition() { return definition; }

    public void setId(Long id) { this.id = id; }
    public void setTerm(String term) { this.term = term; }
    public void setDefinition(String definition) { this.definition = definition; }

    
}

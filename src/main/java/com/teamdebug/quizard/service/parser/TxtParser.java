package com.teamdebug.quizard.service.parser;

public class TxtParser implements FileParser {
    
    @Override
    public String parse(byte[] fileBytes) {
        // TXT parsing logic goes here
        return new String(fileBytes);
    }
    
}

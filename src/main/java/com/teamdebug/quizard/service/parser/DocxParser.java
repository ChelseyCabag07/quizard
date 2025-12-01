package com.teamdebug.quizard.service.parser;

public class DocxParser implements FileParser {

    @Override
     public String parse(byte[] fileBytes) {
        // Later: Apache POI
        return "Parsed DOCX content...";
    }

}

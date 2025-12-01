package com.teamdebug.quizard.service.parser;

public class ParserFactory {
    
       public static FileParser getParser(String fileName) {

        String lower = fileName.toLowerCase();

        if (lower.endsWith(".pdf")) {
            return new PdfParser();
        }
        if (lower.endsWith(".docx")) {
            return new DocxParser();
        }
        if (lower.endsWith(".txt")) {
            return new TxtParser();
        }

        throw new IllegalArgumentException("Unsupported file type: " + fileName);
    }
}

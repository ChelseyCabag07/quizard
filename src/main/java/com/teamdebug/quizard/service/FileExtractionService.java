package com.teamdebug.quizard.service;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class FileExtractionService {
    
    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("File name is null or empty");
        }
        
        String fileExtension = getFileExtension(fileName).toLowerCase();
        
        if ("txt".equals(fileExtension)) {
            return extractFromTxt(file);
        } else if ("docx".equals(fileExtension) || "doc".equals(fileExtension)) {
            return extractFromDocx(file);
        } else {
            throw new IOException("Unsupported file type: " + fileExtension);
        }
    }
    
    private String extractFromTxt(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String text = new String(bytes, StandardCharsets.UTF_8);
        
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        
        return text.trim();
    }
    
    private String extractFromDocx(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            
            return extractor.getText().trim();
        }
    }
    
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : fileName.substring(lastDotIndex + 1);
    }
}
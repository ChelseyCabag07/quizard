package com.teamdebug.quizard.service;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class FileExtractionService {
    
    /**
     * Extract text from uploaded file based on file type
     */
    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("File name is null or empty");
        }
        
        String fileExtension = getFileExtension(fileName).toLowerCase();
        
        switch (fileExtension) {
            case "txt":
                return extractFromTxt(file);
            case "docx":
            case "doc":
                return extractFromDocx(file);
            case "pdf":
                return extractFromPdf(file);
            default:
                throw new IOException("Unsupported file type: " + fileExtension + 
                    ". Supported types: TXT, DOCX, PDF");
        }
    }
    
    /**
     * Extract text from TXT file
     */
    private String extractFromTxt(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        // Try UTF-8 first
        String text = new String(bytes, StandardCharsets.UTF_8);
        
        // Clean any BOM (Byte Order Mark) characters
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        
        return text;
    }
    
    /**
     * Extract text from DOCX file using Apache POI
     */
    private String extractFromDocx(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            
            String text = extractor.getText();
            
            // Clean up the text
            return cleanText(text);
        } catch (Exception e) {
            throw new IOException("Failed to extract text from DOCX file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract text from PDF file using PDFBox
     */
    private String extractFromPdf(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            return cleanText(text);
        } catch (Exception e) {
            throw new IOException("Failed to extract text from PDF file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Clean extracted text from common issues
     */
    private String cleanText(String text) {
        if (text == null) return "";
        
        // Remove excessive whitespace
        text = text.replaceAll("\\s+", " ");
        
        // Remove BOM if present
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        
        // Trim
        return text.trim();
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
}

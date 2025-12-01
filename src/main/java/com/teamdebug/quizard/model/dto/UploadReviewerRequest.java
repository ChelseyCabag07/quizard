package com.teamdebug.quizard.model.dto;

import org.springframework.web.multipart.MultipartFile;

public class UploadReviewerRequest {
    
     private MultipartFile file;

    public UploadReviewerRequest() {}

    public UploadReviewerRequest(MultipartFile file) {
        this.file = file;
    }

    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
}

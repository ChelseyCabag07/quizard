package com.teamdebug.quizard.controller;

import com.teamdebug.quizard.model.dto.SummarizeRequest;
import com.teamdebug.quizard.model.dto.SummaryResponse;
import com.teamdebug.quizard.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/summary")
@CrossOrigin(origins = "*")
public class SummaryController {
    
    @Autowired
    private SummaryService summaryService;
    
    @PostMapping("/generate")
    public ResponseEntity<SummaryResponse> generateSummary(@RequestBody SummarizeRequest request) {
        try {
            System.out.println("Received summary request:");
            System.out.println("Text length: " + request.getText().length());
            
            String summary = summaryService.summarize(request.getText());
            
            return ResponseEntity.ok(new SummaryResponse(summary));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new SummaryResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SummaryResponse("Error generating summary: " + e.getMessage()));
        }
    }
}
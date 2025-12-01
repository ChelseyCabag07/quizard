package com.teamdebug.quizard.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
public class QuizController {
    
    @GetMapping("/test")
    public String test() {
        return "Backend is connected!";
    }
    
    // Your other endpoints here
}
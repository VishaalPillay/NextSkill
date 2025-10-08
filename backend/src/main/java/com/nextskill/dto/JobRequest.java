package com.nextskill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class JobRequest {
    
    @NotBlank(message = "Job description text is required")
    @Size(max = 10000, message = "Job description must be less than 10000 characters")
    private String text;
    
    public JobRequest() {}
    
    public JobRequest(String text) {
        this.text = text;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
}
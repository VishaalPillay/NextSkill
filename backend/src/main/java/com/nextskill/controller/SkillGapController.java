package com.nextskill.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.nextskill.service.SecureHttpClient;
import com.nextskill.repository.ResumeRepository;
import com.nextskill.model.Resume;
import com.nextskill.model.ResumeSkill;
import com.nextskill.service.HybridSkillGapService;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import com.nextskill.dto.JobRequest;
import org.apache.commons.text.StringEscapeUtils;

@RestController
@RequestMapping("/api/skill-gap")
public class SkillGapController {
    
    @Autowired
    private HybridSkillGapService hybridSkillGapService;
    
    @Autowired
    private ResumeRepository resumeRepository;
    
    @Autowired
    private SecureHttpClient secureHttpClient;
    
    @PostMapping("/parse-job")
    public ResponseEntity<?> parseJobDescription(@Valid @RequestBody JobRequest request) {
        try {
            String sanitizedText = StringEscapeUtils.escapeHtml4(request.getText());
            Map<String, String> requestMap = Map.of("text", sanitizedText);
            
            String nlpServiceUrl = "http://localhost:5000/parse-job";
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestMap);
            
            ResponseEntity<Map> response = secureHttpClient.postToAllowedUrl(nlpServiceUrl, entity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error parsing job description: " + e.getMessage()));
        }
    }
    
    @PostMapping("/compare/{resumeId}")
    public ResponseEntity<?> compareSkills(@PathVariable Long resumeId, @Valid @RequestBody JobRequest request) {
        try {
            // Get resume skills
            Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
            
            List<String> resumeSkills = resume.getSkills().stream()
                .map(ResumeSkill::getSkillName)
                .collect(Collectors.toList());
            
            String jobDescription = StringEscapeUtils.escapeHtml4(request.getText());
            
            // Use hybrid service for skill gap analysis
            Map<String, Object> result = hybridSkillGapService.analyzeSkillGap(jobDescription, resumeSkills);
            
            // Add resume info to response
            result.put("resumeId", resumeId);
            result.put("resumeName", resume.getFullName());
            result.put("totalResumeSkills", resumeSkills.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error comparing skills: " + e.getMessage()));
        }
    }
    
    @GetMapping("/resume/{resumeId}/skills")
    public ResponseEntity<?> getResumeSkills(@PathVariable Long resumeId) {
        try {
            Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
            
            List<String> skills = resume.getSkills().stream()
                .map(ResumeSkill::getSkillName)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "resumeId", resumeId,
                "resumeName", resume.getFullName(),
                "skills", skills,
                "totalSkills", skills.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error fetching resume skills: " + e.getMessage()));
        }
    }
}

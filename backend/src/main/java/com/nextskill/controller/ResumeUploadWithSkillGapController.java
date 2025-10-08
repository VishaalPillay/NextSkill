package com.nextskill.controller;

import com.nextskill.model.Resume;
import com.nextskill.model.ResumeSkill;
import com.nextskill.service.ResumeService;
import com.nextskill.service.HybridSkillGapService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.exception.TikaException;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeUploadWithSkillGapController {

    private static final Logger logger = LoggerFactory.getLogger(ResumeUploadWithSkillGapController.class);

    private final ResumeService resumeService;
    private final HybridSkillGapService hybridSkillGapService;

    @PostMapping("/upload-and-analyze")
    public ResponseEntity<Map<String, Object>> uploadResumeAndAnalyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "jobDescription", required = false) String jobDescription) {
        
        // --- VALIDATION ---
        if (file.isEmpty()) {
            return buildErrorResponse("Please select a file to upload.");
        }

        // File size validation (10MB limit)
        if (file.getSize() > 10 * 1024 * 1024) {
            return buildErrorResponse("File size must be less than 10MB.");
        }

        String contentType = file.getContentType();
        if (!isValidFileType(contentType)) {
            return buildErrorResponse("Only PDF and DOCX files are allowed.");
        }

        // Job description validation
        if (jobDescription != null && jobDescription.length() > 10000) {
            return buildErrorResponse("Job description must be less than 10,000 characters.");
        }

        // --- PROCESSING ---
        try {
            // Step 1: Process and save the resume (existing functionality)
            Resume savedResume = resumeService.processAndSaveResume(file);

            // Build basic response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Resume uploaded and parsed successfully.");
            response.put("resumeId", savedResume.getId());
            response.put("resumeName", savedResume.getFullName());
            response.put("extractedSkills", savedResume.getSkills().stream()
                .map(ResumeSkill::getSkillName)
                .collect(Collectors.toList()));

            // Step 2: If job description provided, perform skill gap analysis
            if (jobDescription != null && !jobDescription.trim().isEmpty()) {
                List<String> resumeSkills = savedResume.getSkills().stream()
                    .map(ResumeSkill::getSkillName)
                    .collect(Collectors.toList());

                Map<String, Object> skillGapAnalysis = hybridSkillGapService.analyzeSkillGap(
                    jobDescription, resumeSkills);
                
                response.put("skillGapAnalysis", skillGapAnalysis);
                response.put("message", "Resume uploaded, parsed, and skill gap analysis completed.");
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("File processing error", e);
            return buildErrorResponse("Error reading the uploaded file");
        } catch (TikaException e) {
            logger.error("Text extraction error", e);
            return buildErrorResponse("Error extracting text from file");
        } catch (Exception e) {
            logger.error("Unexpected error during resume upload and analysis", e);
            return buildErrorResponse("An unexpected error occurred");
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file) {
        // Keep original upload functionality for backward compatibility
        if (file.isEmpty()) {
            return buildErrorResponse("Please select a file to upload.");
        }

        String contentType = file.getContentType();
        if (!isValidFileType(contentType)) {
            return buildErrorResponse("Only PDF and DOCX files are allowed.");
        }

        try {
            Resume savedResume = resumeService.processAndSaveResume(file);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Resume uploaded and parsed successfully.");
            response.put("resumeId", savedResume.getId());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("File processing error", e);
            return buildErrorResponse("Error reading the uploaded file");
        } catch (TikaException e) {
            logger.error("Text extraction error", e);
            return buildErrorResponse("Error extracting text from file");
        } catch (Exception e) {
            logger.error("Unexpected error during resume upload", e);
            return buildErrorResponse("An unexpected error occurred");
        }
    }

    private boolean isValidFileType(String contentType) {
        return "application/pdf".equals(contentType) ||
               "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        if (message.contains("unexpected")) {
            return ResponseEntity.internalServerError().body(errorResponse);
        }
        return ResponseEntity.badRequest().body(errorResponse);
    }
}

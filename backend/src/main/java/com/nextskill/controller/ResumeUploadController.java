package com.nextskill.controller;

import com.nextskill.model.Resume;
import com.nextskill.model.ResumeSkill;
import com.nextskill.repository.ResumeSkillRepository;
import com.nextskill.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced Resume Upload Controller with complete Phase 1 functionality
 * Provides REST API endpoints for resume upload, parsing, and data retrieval
 */
@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "*") // Allow cross-origin requests from frontend
@RequiredArgsConstructor
@Slf4j
public class ResumeUploadController {

    private final ResumeService resumeService;
    private final ResumeSkillRepository resumeSkillRepository;

    /**
     * Upload and process resume file
     * POST /api/resume/upload
     * 
     * @param file Resume file (PDF or DOCX)
     * @return JSON response with upload status and resume details
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file) {
        log.info("Received resume upload request: {}", file.getOriginalFilename());
        
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file input
            if (file.isEmpty()) {
                return createErrorResponse("Please select a file to upload", HttpStatus.BAD_REQUEST);
            }

            // Check file type (only PDF and DOCX allowed)
            if (!isValidFileType(file.getContentType())) {
                return createErrorResponse("Only PDF and DOCX files are allowed", HttpStatus.BAD_REQUEST);
            }

            // Check file size (limit to 10MB as configured in application.properties)
            if (file.getSize() > 10 * 1024 * 1024) {
                return createErrorResponse("File size must be less than 10MB", HttpStatus.BAD_REQUEST);
            }

            // Process the resume file through the complete pipeline
            Resume savedResume = resumeService.processAndSaveResume(file);

            // Prepare success response with parsed data
            response.put("status", "success");
            response.put("message", "Resume uploaded and processed successfully");
            response.put("resumeId", savedResume.getId());
            response.put("fileName", savedResume.getOriginalFileName());
            response.put("parsingStatus", savedResume.getParsingStatus());
            
            // Include parsed data if available
            if (savedResume.isParsingCompleted()) {
                response.put("parsedData", buildParsedDataResponse(savedResume));
            } else if (savedResume.hasParsingErrors()) {
                response.put("parsingError", savedResume.getParsingError());
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("File reading error for: " + file.getOriginalFilename(), e);
            return createErrorResponse("Error reading file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
            
        } catch (TikaException e) {
            log.error("Text extraction error for: " + file.getOriginalFilename(), e);
            return createErrorResponse("Error extracting text from file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
            
        } catch (Exception e) {
            log.error("Unexpected error processing: " + file.getOriginalFilename(), e);
            return createErrorResponse("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get resume details with parsed information
     * GET /api/resume/{id}
     * 
     * @param id Resume ID
     * @return JSON response with complete resume details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getResume(@PathVariable Long id) {
        try {
            Resume resume = resumeService.getResumeWithSkills(id);
            Map<String, Object> response = new HashMap<>();
            
            response.put("status", "success");
            response.put("resume", buildCompleteResumeResponse(resume));
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return createErrorResponse("Resume not found with ID: " + id, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error retrieving resume with ID: " + id, e);
            return createErrorResponse("Error retrieving resume: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all successfully parsed resumes
     * GET /api/resume/list
     * 
     * @return JSON response with list of all parsed resumes
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllResumes() {
        try {
            List<Resume> resumes = resumeService.getAllSuccessfullyParsedResumes();
            Map<String, Object> response = new HashMap<>();
            
            List<Map<String, Object>> resumeList = resumes.stream()
                .map(this::buildResumeListItem)
                .toList();
            
            response.put("status", "success");
            response.put("resumes", resumeList);
            response.put("count", resumeList.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving resume list", e);
            return createErrorResponse("Error retrieving resumes: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get skills for a specific resume
     * GET /api/resume/{id}/skills
     * 
     * @param id Resume ID
     * @return JSON response with resume skills grouped by category
     */
    @GetMapping("/{id}/skills")
    public ResponseEntity<Map<String, Object>> getResumeSkills(@PathVariable Long id) {
        try {
            List<ResumeSkill> skills = resumeSkillRepository.findByResumeId(id);
            List<String> categories = resumeSkillRepository.findDistinctCategoriesByResumeId(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("resumeId", id);
            response.put("totalSkills", skills.size());
            response.put("categories", categories);
            
            // Group skills by category
            Map<String, List<Map<String, Object>>> skillsByCategory = new HashMap<>();
            for (String category : categories) {
                List<ResumeSkill> categorySkills = resumeSkillRepository.findByResumeIdAndSkillCategory(id, category);
                List<Map<String, Object>> skillList = categorySkills.stream()
                    .map(this::buildSkillResponse)
                    .toList();
                skillsByCategory.put(category, skillList);
            }
            
            response.put("skillsByCategory", skillsByCategory);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving skills for resume ID: " + id, e);
            return createErrorResponse("Error retrieving skills: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get resume statistics and analytics
     * GET /api/resume/{id}/stats
     * 
     * @param id Resume ID
     * @return JSON response with resume statistics
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getResumeStats(@PathVariable Long id) {
        try {
            ResumeService.ResumeStats stats = resumeService.getResumeStatistics(id);
            Map<String, Object> response = new HashMap<>();
            
            response.put("status", "success");
            response.put("stats", Map.of(
                "resumeId", stats.getResumeId(),
                "totalSkills", stats.getTotalSkills(),
                "highConfidenceSkills", stats.getHighConfidenceSkills(),
                "skillCategories", stats.getSkillCategories(),
                "hasContactInfo", stats.isHasContactInfo(),
                "parsingStatus", stats.getParsingStatus(),
                "confidenceRatio", stats.getTotalSkills() > 0 ? 
                    (double) stats.getHighConfidenceSkills() / stats.getTotalSkills() : 0.0
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving stats for resume ID: " + id, e);
            return createErrorResponse("Error retrieving statistics: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reparse an existing resume (useful when NLP models are updated)
     * POST /api/resume/{id}/reparse
     * 
     * @param id Resume ID to reparse
     * @return JSON response with reparse results
     */
    @PostMapping("/{id}/reparse")
    public ResponseEntity<Map<String, Object>> reparseResume(@PathVariable Long id) {
        try {
            Resume reparsedResume = resumeService.reparseResume(id);
            Map<String, Object> response = new HashMap<>();
            
            response.put("status", "success");
            response.put("message", "Resume reparsed successfully");
            response.put("resumeId", reparsedResume.getId());
            response.put("parsingStatus", reparsedResume.getParsingStatus());
            
            if (reparsedResume.isParsingCompleted()) {
                response.put("parsedData", buildParsedDataResponse(reparsedResume));
            } else if (reparsedResume.hasParsingErrors()) {
                response.put("parsingError", reparsedResume.getParsingError());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return createErrorResponse("Resume not found with ID: " + id, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error reparsing resume ID: " + id, e);
            return createErrorResponse("Error reparsing resume: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Health check endpoint to verify service is running
     * GET /api/resume/health
     * 
     * @return Simple health check response
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "NextSkill Resume Parser");
        response.put("version", "1.0.0");
        response.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    // Helper Methods

    /**
     * Check if file type is valid (PDF or DOCX)
     */
    private boolean isValidFileType(String contentType) {
        return "application/pdf".equals(contentType) ||
               "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType);
    }

    /**
     * Create error response with consistent format
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        error.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Build parsed data response for successful parsing
     */
    private Map<String, Object> buildParsedDataResponse(Resume resume) {
        Map<String, Object> parsedData = new HashMap<>();
        
        parsedData.put("fullName", resume.getFullName());
        parsedData.put("email", resume.getEmail());
        parsedData.put("phoneNumber", resume.getPhoneNumber());
        parsedData.put("summary", resume.getSummary());
        parsedData.put("yearsOfExperience", resume.getYearsOfExperience());
        
        // Get skills count by category
        if (resume.getSkills() != null) {
            Map<String, Long> skillCounts = resume.getSkills().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    skill -> skill.getSkillCategory() != null ? skill.getSkillCategory() : "Uncategorized",
                    java.util.stream.Collectors.counting()
                ));
            
            parsedData.put("skillCounts", skillCounts);
            parsedData.put("totalSkills", resume.getSkills().size());
        }
        
        return parsedData;
    }

    /**
     * Build complete resume response with all details
     */
    private Map<String, Object> buildCompleteResumeResponse(Resume resume) {
        Map<String, Object> resumeData = new HashMap<>();
        
        // Basic information
        resumeData.put("id", resume.getId());
        resumeData.put("originalFileName", resume.getOriginalFileName());
        resumeData.put("createdAt", resume.getCreatedAt());
        resumeData.put("parsingStatus", resume.getParsingStatus());
        
        // Parsed personal information
        resumeData.put("fullName", resume.getFullName());
        resumeData.put("email", resume.getEmail());
        resumeData.put("phoneNumber", resume.getPhoneNumber());
        resumeData.put("summary", resume.getSummary());
        resumeData.put("yearsOfExperience", resume.getYearsOfExperience());
        
        // Skills information
        if (resume.getSkills() != null) {
            List<Map<String, Object>> skills = resume.getSkills().stream()
                .map(this::buildSkillResponse)
                .toList();
            resumeData.put("skills", skills);
            
            // Group skills by category for easier frontend consumption
            Map<String, List<Map<String, Object>>> skillsByCategory = resume.getSkills().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    skill -> skill.getSkillCategory() != null ? skill.getSkillCategory() : "Uncategorized",
                    java.util.stream.Collectors.mapping(this::buildSkillResponse, java.util.stream.Collectors.toList())
                ));
            resumeData.put("skillsByCategory", skillsByCategory);
        }
        
        return resumeData;
    }

    /**
     * Build resume list item for the list endpoint
     */
    private Map<String, Object> buildResumeListItem(Resume resume) {
        Map<String, Object> item = new HashMap<>();
        
        item.put("id", resume.getId());
        item.put("originalFileName", resume.getOriginalFileName());
        item.put("fullName", resume.getFullName());
        item.put("email", resume.getEmail());
        item.put("createdAt", resume.getCreatedAt());
        item.put("parsingStatus", resume.getParsingStatus());
        item.put("yearsOfExperience", resume.getYearsOfExperience());
        
        // Add skill count if skills are loaded
        if (resume.getSkills() != null) {
            item.put("skillCount", resume.getSkills().size());
        }
        
        return item;
    }

    /**
     * Build skill response object
     */
    private Map<String, Object> buildSkillResponse(ResumeSkill skill) {
        Map<String, Object> skillData = new HashMap<>();
        
        skillData.put("id", skill.getId());
        skillData.put("skillName", skill.getSkillName());
        skillData.put("category", skill.getSkillCategory());
        skillData.put("confidenceScore", skill.getConfidenceScore());
        skillData.put("isHighConfidence", skill.isHighConfidence());
        skillData.put("createdAt", skill.getCreatedAt());
        
        return skillData;
    }
}
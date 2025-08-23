package com.nextskill.service;

import com.nextskill.dto.ParsedResumeData;
import com.nextskill.model.Resume;
import com.nextskill.model.ResumeSkill;
import com.nextskill.repository.ResumeRepository;
import com.nextskill.repository.ResumeSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Resume Service with NLP integration for complete resume parsing
 * Handles file processing, text extraction, NLP parsing, and data persistence
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final NLPService nlpService;
    
    // Apache Tika for text extraction from various file formats
    private final Tika tika = new Tika();

    /**
     * Main method to process uploaded resume file and extract structured data
     * @param file uploaded resume file (PDF or DOCX)
     * @return Resume entity with parsed structured data
     * @throws IOException if file reading fails
     * @throws TikaException if text extraction fails
     */
    @Transactional
    public Resume processAndSaveResume(MultipartFile file) throws IOException, TikaException {
        log.info("Processing resume file: {}", file.getOriginalFilename());
        
        // Step 1: Extract raw text from file using Apache Tika
        String extractedText = extractTextFromFile(file);
        
        // Step 2: Create initial resume record with raw text
        Resume resume = new Resume(file.getOriginalFilename(), extractedText);
        resume.setParsingStatus(Resume.ParsingStatus.PROCESSING);
        resume = resumeRepository.save(resume); // Save to get ID for foreign key relations
        
        try {
            // Step 3: Parse text using NLP service
            ParsedResumeData parsedData = nlpService.parseResumeText(extractedText);
            
            // Step 4: Update resume with parsed structured data
            updateResumeWithParsedData(resume, parsedData);
            
            // Step 5: Save skills separately in resume_skill table
            saveExtractedSkills(resume, parsedData.getSkills());
            
            // Step 6: Mark parsing as completed
            resume.markParsingCompleted();
            resume = resumeRepository.save(resume);
            
            log.info("Resume processing completed successfully for file: {} (ID: {})", 
                    file.getOriginalFilename(), resume.getId());
            
        } catch (Exception e) {
            // Handle parsing errors gracefully
            log.error("Error during NLP parsing for file: " + file.getOriginalFilename(), e);
            resume.markParsingFailed("NLP parsing failed: " + e.getMessage());
            resume = resumeRepository.save(resume);
        }
        
        return resume;
    }

    /**
     * Extract text content from uploaded file using Apache Tika
     * @param file multipart file to extract text from
     * @return cleaned extracted text
     * @throws IOException if file reading fails
     * @throws TikaException if text extraction fails
     */
    private String extractTextFromFile(MultipartFile file) throws IOException, TikaException {
        log.debug("Extracting text from file: {}", file.getOriginalFilename());
        
        // Use Tika to extract raw text
        String rawText = tika.parseToString(file.getInputStream());
        
        // Clean and normalize the extracted text
        String cleanedText = rawText
            .replaceAll("\\s+", " ") // Replace multiple whitespace with single space
            .replaceAll("\\p{Cntrl}", "") // Remove control characters
            .trim();
        
        log.debug("Extracted {} characters from file", cleanedText.length());
        
        if (cleanedText.length() < 50) {
            throw new TikaException("Extracted text is too short, file might be corrupted or unreadable");
        }
        
        return cleanedText;
    }

    /**
     * Update resume entity with parsed structured data
     * @param resume resume entity to update
     * @param parsedData parsed data from NLP service
     */
    private void updateResumeWithParsedData(Resume resume, ParsedResumeData parsedData) {
        if (parsedData == null || !parsedData.hasValidData()) {
            log.warn("No valid parsed data available for resume ID: {}", resume.getId());
            return;
        }
        
        // Update personal information
        resume.setFullName(parsedData.getFullName());
        resume.setEmail(parsedData.getEmail());
        resume.setPhoneNumber(parsedData.getPhoneNumber());
        resume.setSummary(parsedData.getSummary());
        resume.setYearsOfExperience(parsedData.getYearsOfExperience());
        
        log.debug("Updated resume {} with parsed data: name={}, email={}, skills_count={}", 
                resume.getId(), parsedData.getFullName(), parsedData.getEmail(), 
                parsedData.getSkills() != null ? parsedData.getSkills().size() : 0);
    }

    /**
     * Save extracted skills to the resume_skill table
     * @param resume parent resume entity
     * @param skillsData list of extracted skills with categories and confidence scores
     */
    @Transactional
    private void saveExtractedSkills(Resume resume, List<ParsedResumeData.SkillData> skillsData) {
        if (skillsData == null || skillsData.isEmpty()) {
            log.info("No skills found for resume ID: {}", resume.getId());
            return;
        }
        
        List<ResumeSkill> skillEntities = new ArrayList<>();
        
        for (ParsedResumeData.SkillData skillData : skillsData) {
            // Create ResumeSkill entity
            ResumeSkill resumeSkill = new ResumeSkill(
                resume,
                skillData.getSkillName(),
                skillData.getCategory(),
                BigDecimal.valueOf(skillData.getConfidenceScore())
            );
            
            skillEntities.add(resumeSkill);
        }
        
        // Batch save all skills
        List<ResumeSkill> savedSkills = resumeSkillRepository.saveAll(skillEntities);
        
        log.info("Saved {} skills for resume ID: {}", savedSkills.size(), resume.getId());
    }

    /**
     * Retrieve resume with all associated skills
     * @param resumeId ID of the resume to retrieve
     * @return Resume entity with skills loaded
     */
    public Resume getResumeWithSkills(Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
            .orElseThrow(() -> new RuntimeException("Resume not found with ID: " + resumeId));
        
        // Load skills separately (due to LAZY loading)
        List<ResumeSkill> skills = resumeSkillRepository.findByResumeId(resumeId);
        resume.setSkills(skills);
        
        return resume;
    }

    /**
     * Get all successfully parsed resumes
     * @return list of resumes with completed parsing status
     */
    public List<Resume> getAllSuccessfullyParsedResumes() {
        return resumeRepository.findAll().stream()
            .filter(Resume::isParsingCompleted)
            .toList();
    }

    /**
     * Reparse an existing resume (useful if NLP models are updated)
     * @param resumeId ID of resume to reparse
     * @return updated resume with new parsing results
     */
    @Transactional
    public Resume reparseResume(Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
            .orElseThrow(() -> new RuntimeException("Resume not found with ID: " + resumeId));
        
        log.info("Re-parsing resume ID: {}", resumeId);
        
        // Delete existing skills
        resumeSkillRepository.deleteByResumeId(resumeId);
        
        // Reset parsing status
        resume.setParsingStatus(Resume.ParsingStatus.PROCESSING);
        resume.setParsingError(null);
        
        try {
            // Re-parse the existing extracted text
            ParsedResumeData parsedData = nlpService.parseResumeText(resume.getExtractedText());
            
            // Update with new parsed data
            updateResumeWithParsedData(resume, parsedData);
            saveExtractedSkills(resume, parsedData.getSkills());
            
            resume.markParsingCompleted();
            
        } catch (Exception e) {
            log.error("Error during re-parsing resume ID: " + resumeId, e);
            resume.markParsingFailed("Re-parsing failed: " + e.getMessage());
        }
        
        return resumeRepository.save(resume);
    }

    /**
     * Get resume statistics for analytics
     * @param resumeId ID of resume
     * @return map with various statistics
     */
    public ResumeStats getResumeStatistics(Long resumeId) {
        Resume resume = getResumeWithSkills(resumeId);
        
        return ResumeStats.builder()
            .resumeId(resumeId)
            .totalSkills(resume.getSkills() != null ? resume.getSkills().size() : 0)
            .highConfidenceSkills(resume.getSkills() != null ? 
                (int) resume.getSkills().stream().filter(ResumeSkill::isHighConfidence).count() : 0)
            .skillCategories(resumeSkillRepository.findDistinctCategoriesByResumeId(resumeId))
            .hasContactInfo(resume.getEmail() != null || resume.getPhoneNumber() != null)
            .parsingStatus(resume.getParsingStatus().toString())
            .build();
    }

    /**
     * Inner class for resume statistics
     */
    @lombok.Builder
    @lombok.Data
    public static class ResumeStats {
        private Long resumeId;
        private int totalSkills;
        private int highConfidenceSkills;
        private List<String> skillCategories;
        private boolean hasContactInfo;
        private String parsingStatus;
    }
}
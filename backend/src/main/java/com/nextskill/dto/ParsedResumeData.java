package com.nextskill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for holding parsed resume data
 * This DTO is used to pass structured data between NLP service and other components
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedResumeData {

    // Personal information
    private String fullName;
    private String email;
    private String phoneNumber;
    
    // Professional information
    private String summary;
    private Integer yearsOfExperience;
    
    // Skills with categories and confidence scores
    private List<SkillData> skills;
    
    // Processing metadata
    private boolean parsingSuccessful;
    private String errorMessage;
    private double overallConfidence; // Average confidence across all extracted data

    /**
     * Inner class to represent individual skill data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillData {
        private String skillName;
        private String category;
        private double confidenceScore;
        
        public SkillData(String skillName, String category) {
            this.skillName = skillName;
            this.category = category;
            this.confidenceScore = 1.0; // Default high confidence for manually categorized skills
        }
    }

    // Utility methods for easier data manipulation
    
    /**
     * Check if the parsing was successful and contains valid data
     */
    public boolean hasValidData() {
        return parsingSuccessful && 
               (fullName != null || email != null || (skills != null && !skills.isEmpty()));
    }

    /**
     * Get count of high-confidence skills (confidence >= 0.7)
     */
    public long getHighConfidenceSkillCount() {
        if (skills == null) return 0;
        return skills.stream()
                .mapToDouble(SkillData::getConfidenceScore)
                .filter(score -> score >= 0.7)
                .count();
    }

    /**
     * Check if contact information is available
     */
    public boolean hasContactInfo() {
        return email != null || phoneNumber != null;
    }

    /**
     * Get skills by category
     */
    public List<SkillData> getSkillsByCategory(String category) {
        if (skills == null) return List.of();
        return skills.stream()
                .filter(skill -> category.equalsIgnoreCase(skill.getCategory()))
                .toList();
    }

    /**
     * Add a skill to the skills list
     */
    public void addSkill(String skillName, String category, double confidenceScore) {
        if (skills == null) {
            skills = new java.util.ArrayList<>();
        }
        skills.add(new SkillData(skillName, category, confidenceScore));
    }

    /**
     * Set parsing as failed with error message
     */
    public void setParsingFailed(String errorMessage) {
        this.parsingSuccessful = false;
        this.errorMessage = errorMessage;
        this.overallConfidence = 0.0;
    }

    /**
     * Set parsing as successful
     */
    public void setParsingSuccessful() {
        this.parsingSuccessful = true;
        this.errorMessage = null;
        calculateOverallConfidence();
    }

    /**
     * Calculate overall confidence based on extracted data
     */
    private void calculateOverallConfidence() {
        double totalConfidence = 0.0;
        int dataPoints = 0;

        // Count confidence for personal info (assume high confidence if present)
        if (fullName != null && !fullName.trim().isEmpty()) {
            totalConfidence += 0.9;
            dataPoints++;
        }
        if (email != null && !email.trim().isEmpty()) {
            totalConfidence += 0.95;
            dataPoints++;
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            totalConfidence += 0.85;
            dataPoints++;
        }

        // Add skills confidence
        if (skills != null && !skills.isEmpty()) {
            double avgSkillConfidence = skills.stream()
                    .mapToDouble(SkillData::getConfidenceScore)
                    .average()
                    .orElse(0.0);
            totalConfidence += avgSkillConfidence;
            dataPoints++;
        }

        this.overallConfidence = dataPoints > 0 ? totalConfidence / dataPoints : 0.0;
    }
}
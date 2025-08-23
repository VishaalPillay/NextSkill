package com.nextskill.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resume entity representing a parsed resume with structured data
 * This entity stores both raw extracted text and parsed structured information
 */
@Entity
@Table(name = "resume")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Original file information
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    // Parsed structured data
    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience = 0;

    // Processing status tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "parsing_status")
    private ParsingStatus parsingStatus = ParsingStatus.PENDING;

    @Column(name = "parsing_error", columnDefinition = "TEXT")
    private String parsingError;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ResumeSkill> skills;

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Custom constructors for different use cases
    public Resume(String originalFileName, String extractedText) {
        this.originalFileName = originalFileName;
        this.extractedText = extractedText;
        this.parsingStatus = ParsingStatus.PENDING;
    }

    public Resume(String originalFileName, String extractedText, String fullName, 
                 String email, String phoneNumber, String summary, Integer yearsOfExperience) {
        this.originalFileName = originalFileName;
        this.extractedText = extractedText;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.summary = summary;
        this.yearsOfExperience = yearsOfExperience;
        this.parsingStatus = ParsingStatus.COMPLETED;
    }

    // Utility methods
    public boolean isParsingCompleted() {
        return ParsingStatus.COMPLETED.equals(this.parsingStatus);
    }

    public boolean hasParsingErrors() {
        return ParsingStatus.FAILED.equals(this.parsingStatus);
    }

    public void markParsingCompleted() {
        this.parsingStatus = ParsingStatus.COMPLETED;
        this.parsingError = null;
    }

    public void markParsingFailed(String error) {
        this.parsingStatus = ParsingStatus.FAILED;
        this.parsingError = error;
    }

    // Enum for parsing status
    public enum ParsingStatus {
        PENDING,    // Resume uploaded but not yet parsed
        PROCESSING, // Currently being processed by NLP
        COMPLETED,  // Successfully parsed
        FAILED      // Parsing failed with errors
    }
}
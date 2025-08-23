package com.nextskill.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ResumeSkill entity representing individual skills extracted from a resume
 * Each skill has a confidence score indicating NLP parsing confidence
 */
@Entity
@Table(name = "resume_skill")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign key reference to Resume
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(name = "skill_category")
    private String skillCategory;

    // Confidence score from NLP parsing (0.00 to 1.00)
    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructor without ID and timestamp (for new skills)
    public ResumeSkill(Resume resume, String skillName, String skillCategory, BigDecimal confidenceScore) {
        this.resume = resume;
        this.skillName = skillName;
        this.skillCategory = skillCategory;
        this.confidenceScore = confidenceScore;
    }

    // Constructor with confidence score as double (convenience)
    public ResumeSkill(Resume resume, String skillName, String skillCategory, double confidenceScore) {
        this(resume, skillName, skillCategory, BigDecimal.valueOf(confidenceScore));
    }

    // Utility methods
    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore.compareTo(BigDecimal.valueOf(0.7)) >= 0;
    }

    public boolean isLowConfidence() {
        return confidenceScore != null && confidenceScore.compareTo(BigDecimal.valueOf(0.3)) < 0;
    }

    // Clean skill name (remove extra spaces, standardize case)
    public void normalizeSkillName() {
        if (skillName != null) {
            this.skillName = skillName.trim().toLowerCase();
        }
    }
}
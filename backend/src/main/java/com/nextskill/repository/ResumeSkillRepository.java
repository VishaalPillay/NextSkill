package com.nextskill.repository;

import com.nextskill.model.ResumeSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository interface for ResumeSkill entity
 * Provides database operations for resume skills with custom query methods
 */
@Repository
public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, Long> {

    /**
     * Find all skills for a specific resume
     * @param resumeId the resume ID
     * @return list of skills for the resume
     */
    List<ResumeSkill> findByResumeId(Long resumeId);

    /**
     * Find skills by category for a specific resume
     * @param resumeId the resume ID
     * @param skillCategory the skill category to filter by
     * @return list of skills in the specified category
     */
    List<ResumeSkill> findByResumeIdAndSkillCategory(Long resumeId, String skillCategory);

    /**
     * Find high-confidence skills for a resume (confidence >= threshold)
     * @param resumeId the resume ID
     * @param confidenceThreshold minimum confidence score
     * @return list of high-confidence skills
     */
    @Query("SELECT rs FROM ResumeSkill rs WHERE rs.resume.id = :resumeId " +
           "AND rs.confidenceScore >= :threshold ORDER BY rs.confidenceScore DESC")
    List<ResumeSkill> findHighConfidenceSkills(@Param("resumeId") Long resumeId, 
                                              @Param("threshold") BigDecimal confidenceThreshold);

    /**
     * Find all distinct skill categories for a resume
     * @param resumeId the resume ID
     * @return list of distinct skill categories
     */
    @Query("SELECT DISTINCT rs.skillCategory FROM ResumeSkill rs WHERE rs.resume.id = :resumeId " +
           "AND rs.skillCategory IS NOT NULL ORDER BY rs.skillCategory")
    List<String> findDistinctCategoriesByResumeId(@Param("resumeId") Long resumeId);

    /**
     * Count skills by category for a resume
     * @param resumeId the resume ID
     * @param category the skill category
     * @return count of skills in the category
     */
    @Query("SELECT COUNT(rs) FROM ResumeSkill rs WHERE rs.resume.id = :resumeId " +
           "AND rs.skillCategory = :category")
    Long countByResumeIdAndCategory(@Param("resumeId") Long resumeId, @Param("category") String category);

    /**
     * Find skills containing a keyword (case-insensitive)
     * @param resumeId the resume ID
     * @param keyword the keyword to search for
     * @return list of matching skills
     */
    @Query("SELECT rs FROM ResumeSkill rs WHERE rs.resume.id = :resumeId " +
           "AND LOWER(rs.skillName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<ResumeSkill> findSkillsContaining(@Param("resumeId") Long resumeId, @Param("keyword") String keyword);

    /**
     * Delete all skills for a specific resume (useful for re-parsing)
     * @param resumeId the resume ID
     * @return number of deleted records
     */
    Long deleteByResumeId(Long resumeId);

    /**
     * Find most common skills across all resumes (for analytics)
     * @param limit maximum number of results
     * @return list of skill names with their occurrence count
     */
    @Query("SELECT rs.skillName, COUNT(rs) as skillCount FROM ResumeSkill rs " +
           "GROUP BY rs.skillName ORDER BY COUNT(rs) DESC LIMIT :limit")
    List<Object[]> findMostCommonSkills(@Param("limit") int limit);
}
package com.nextskill.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "resume_skill")
@Data
@NoArgsConstructor
public class ResumeSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    @ToString.Exclude // Prevents infinite loop in toString()
    private Resume resume;

    public ResumeSkill(String skillName) {
        this.skillName = skillName;
    }
}
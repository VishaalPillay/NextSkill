package com.nextskill.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nextskill.model.Resume;

public class NLPServiceTest {

    private NLPService nlpService;

    @BeforeEach
    public void setUp() {
        nlpService = new NLPService();
    }

    @Test
    public void testBasicResumeParsing() {
        String sampleResume = """
            John Doe
            Email: john.doe@example.com
            Phone: +1-555-123-4567

            Skills
            Java, Python, Spring Boot, Docker

            Experience
            Acme Corp
            Software Engineer
            Jan 2020 - Present
            """;

        Resume resume = nlpService.parseResumeText(sampleResume);
        
        System.out.println("=== BASIC RESUME PARSING TEST ===");
        System.out.println(resume);
        
        assertNotNull(resume);
        assertNotNull(resume.getFullName());
        assertFalse(resume.getSkills().isEmpty());
        assertNotNull(resume.getEmail());
        assertNotNull(resume.getPhoneNumber());
    }

    @Test
    public void testNameExtraction() {
        String resumeText = """
            Alice Johnson
            alice.johnson@email.com
            (555) 123-4567
            
            Skills: Java, Python
            """;
        
        Resume resume = nlpService.parseResumeText(resumeText);
        System.out.println("=== NAME EXTRACTION TEST ===");
        System.out.println("Extracted name: " + resume.getFullName());
        
        assertNotNull(resume.getFullName());
        assertTrue(resume.getFullName().contains("Alice") || resume.getFullName().contains("Johnson"));
    }

    @Test
    public void testEmailExtraction() {
        String resumeText = """
            Bob Smith
            bob.smith@gmail.com
            +1-555-987-6543
            """;
        
        Resume resume = nlpService.parseResumeText(resumeText);
        System.out.println("=== EMAIL EXTRACTION TEST ===");
        System.out.println("Extracted email: " + resume.getEmail());
        
        assertNotNull(resume.getEmail());
        assertTrue(resume.getEmail().contains("@"));
    }

    @Test
    public void testPhoneExtraction() {
        String resumeText = """
            Carol Wilson
            carol@example.com
            +1 (555) 456-7890
            """;
        
        Resume resume = nlpService.parseResumeText(resumeText);
        System.out.println("=== PHONE EXTRACTION TEST ===");
        System.out.println("Extracted phone: " + resume.getPhoneNumber());
        
        assertNotNull(resume.getPhoneNumber());
        assertTrue(resume.getPhoneNumber().contains("555"));
    }

    @Test
    public void testSkillsExtraction() {
        String resumeText = """
            David Brown
            david@test.com
            555-123-4567
            
            Skills:
            Java, Python, React, Docker, AWS, PostgreSQL
            """;
        
        Resume resume = nlpService.parseResumeText(resumeText);
        System.out.println("=== SKILLS EXTRACTION TEST ===");
        System.out.println("Extracted skills: " + resume.getSkills());
        
        assertFalse(resume.getSkills().isEmpty());
        assertTrue(resume.getSkills().size() >= 3);
    }

    @Test
    public void testExperienceExtraction() {
        String resumeText = """
            Emma Davis
            emma@company.com
            555-111-2222
            
            Experience
            
            TechCorp Inc
            Senior Developer
            Mar 2021 - Present
            
            StartupXYZ
            Junior Developer
            Jan 2020 - Feb 2021
            """;
        
        Resume resume = nlpService.parseResumeText(resumeText);
        System.out.println("=== EXPERIENCE EXTRACTION TEST ===");
        System.out.println("Extracted experiences: " + resume.getExperiences());
        
        assertFalse(resume.getExperiences().isEmpty());
        assertTrue(resume.getExperiences().size() >= 1);
    }

    @Test
    public void testProjectsExtraction() {
        String resumeText = """
            Frank Miller
            frank@dev.com
            555-333-4444
            
            Projects
            
            E-commerce Platform
            Jan 2023 - Mar 2023
            Built a full-stack e-commerce application using React and Node.js
            
            Task Manager App
            Nov 2022 - Dec 2022
            Created a task management application with Python and Django
            """;
        
        Resume resume = nlpService.parseResumeText(resumeText);
        System.out.println("=== PROJECTS EXTRACTION TEST ===");
        System.out.println("Extracted projects: " + resume.getProjects());
        
        assertFalse(resume.getProjects().isEmpty());
        assertTrue(resume.getProjects().size() >= 1);
    }

    @Test
    public void testCertificationsExtraction() {
        String resumeText = """
            Grace Lee
            grace@certified.com
            555-555-5555
            
            CERTIFICATIONS
            
            AWS Certified Developer, Amazon Web Services
            Google Cloud Professional Developer, Google
            Microsoft Azure Developer Associate, Microsoft
            """;
        
        Resume resume = nlpService.parseResumeText(resumeText);
        System.out.println("=== CERTIFICATIONS EXTRACTION TEST ===");
        System.out.println("Extracted certifications: " + resume.getCertifications());
        
        assertFalse(resume.getCertifications().isEmpty());
        assertTrue(resume.getCertifications().size() >= 1);
    }

    @Test
    public void testComplexResume() {
        String complexResume = """
            Sarah Johnson
            sarah.johnson@techcompany.com
            +1 (555) 123-4567
            
            Skills
            Java, Spring Boot, Python, React, Docker, Kubernetes, AWS, PostgreSQL, MongoDB
            
            Experience
            
            TechGiant Inc
            Senior Software Engineer
            Jan 2022 - Present
            Led development of microservices architecture
            
            StartupCo
            Full Stack Developer
            Jun 2020 - Dec 2021
            Built REST APIs and frontend applications
            
            Projects
            
            AI Chatbot Platform
            Mar 2023 - Jun 2023
            Developed an AI-powered chatbot using Python and TensorFlow
            
            Microservices Dashboard
            Sep 2022 - Dec 2022
            Created a monitoring dashboard for microservices
            
            CERTIFICATIONS
            
            AWS Solutions Architect, Amazon Web Services
            Kubernetes Administrator, Cloud Native Computing Foundation
            """;
        
        Resume resume = nlpService.parseResumeText(complexResume);
        System.out.println("=== COMPLEX RESUME TEST ===");
        System.out.println(resume);
        
        assertNotNull(resume);
        assertNotNull(resume.getFullName());
        assertNotNull(resume.getEmail());
        assertNotNull(resume.getPhoneNumber());
        assertFalse(resume.getSkills().isEmpty());
        assertFalse(resume.getExperiences().isEmpty());
        assertFalse(resume.getProjects().isEmpty());
        assertFalse(resume.getCertifications().isEmpty());
    }
}

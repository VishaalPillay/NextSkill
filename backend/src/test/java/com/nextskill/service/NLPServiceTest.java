package com.nextskill.service;

import com.nextskill.model.Resume;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class NLPServiceTest {

    @Autowired
    private NLPService nlpService;

    @Test
    public void testParseResumeText() {
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

        // Parse the sample resume text
        Resume resume = nlpService.parseResumeText(sampleResume);

        // --- ADDED LINE TO PRINT THE PARSED DATA ---
        System.out.println(resume);

        // Assertions to verify parsing was successful
        assertNotNull(resume);
        assertNotNull(resume.getFullName());
        assertTrue(resume.getSkills().size() > 0);
    }
}

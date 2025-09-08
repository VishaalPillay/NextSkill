package com.nextskill.service;

import com.nextskill.dto.PythonNLPResponse;
import com.nextskill.model.Resume;
import com.nextskill.model.ResumeSkill;
import com.nextskill.repository.ResumeRepository;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final PythonNLPClient pythonNLPClient; // <-- DEPENDENCY HAS CHANGED
    private final Tika tika = new Tika();

    public ResumeService(ResumeRepository resumeRepository, PythonNLPClient pythonNLPClient) {
        this.resumeRepository = resumeRepository;
        this.pythonNLPClient = pythonNLPClient;
    }

    public Resume processAndSaveResume(MultipartFile file) throws IOException, TikaException {
        // Step 1: Extract raw text with Tika (this part is unchanged)
        String rawText = tika.parseToString(file.getInputStream());

        // Step 2: Call our new Python service to get structured NLP data
        PythonNLPResponse parsedData = pythonNLPClient.parseResumeText(rawText);

        // Step 3: Map the data from the Python service to our Java database models
        Resume resume = new Resume();
        resume.setOriginalFileName(file.getOriginalFilename());
        resume.setFullName(parsedData.getFullName());
        resume.setEmail(parsedData.getEmail());
        resume.setPhoneNumber(parsedData.getPhoneNumber());

        if (parsedData.getSkills() != null) {
            for (PythonNLPResponse.SkillData skillData : parsedData.getSkills()) {
                resume.addSkill(new ResumeSkill(skillData.getSkillName()));
            }
        }
        
        // Step 4: Save the fully structured Resume object to the database.
        return resumeRepository.save(resume);
    }
}
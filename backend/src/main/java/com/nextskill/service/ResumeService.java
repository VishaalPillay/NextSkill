package com.nextskill.service;

import com.nextskill.model.Resume;
import com.nextskill.repository.ResumeRepository;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final Tika tika = new Tika();

    // Constructor injection (no @Autowired needed)
    public ResumeService(ResumeRepository resumeRepository) {
        this.resumeRepository = resumeRepository;
    }

    public Resume processAndSaveResume(MultipartFile file) throws IOException, TikaException {
        // Extract raw text using Apache Tika
        String extractedText = tika.parseToString(file.getInputStream());

        // Clean extracted text: remove extra spaces, tabs, and newlines
        String cleanedText = extractedText.replaceAll("\\s+", " ").trim();

        // Create and save Resume entity
        Resume resume = new Resume(file.getOriginalFilename(), cleanedText);
        return resumeRepository.save(resume);
    }
}

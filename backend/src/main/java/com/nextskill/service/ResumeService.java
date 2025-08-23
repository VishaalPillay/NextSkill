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
    private final NLPService nlpService;
    private final Tika tika;

    // The constructor now correctly injects all required services (ResumeRepository and NLPService).
    public ResumeService(ResumeRepository resumeRepository, NLPService nlpService) {
        this.resumeRepository = resumeRepository;
        this.nlpService = nlpService;
        this.tika = new Tika(); // Initialize Tika here
    }

    /**
     * Processes an uploaded resume file by extracting its text, parsing it for structured data,
     * and saving it to the database.
     *
     * @param file The uploaded MultipartFile.
     * @return The saved Resume entity with a generated ID.
     * @throws IOException   if there is an error reading the file.
     * @throws TikaException if there is an error parsing the file content.
     */
    public Resume processAndSaveResume(MultipartFile file) throws IOException, TikaException {
        // Step 1: Extract raw text content from the file using Apache Tika.
        String rawText = tika.parseToString(file.getInputStream());

        // Step 2: Pass the raw text to the NLPService to get a fully parsed and structured Resume object.
        Resume parsedResume = nlpService.parseResumeText(rawText);

        // Step 3: Set metadata that wasn't available during parsing (like the original filename).
        parsedResume.setOriginalFileName(file.getOriginalFilename());

        // Step 4: Save the complete, structured Resume object to the database via the repository.
        return resumeRepository.save(parsedResume);
    }
}
package com.nextskill.controller;

import com.nextskill.model.Resume;
import com.nextskill.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.exception.TikaException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ResumeUploadController {

    private final ResumeService resumeService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            // Check file type
            String contentType = file.getContentType();
            if (!isValidFileType(contentType)) {
                response.put("status", "error");
                response.put("message", "Only PDF and DOCX files are allowed");
                return ResponseEntity.badRequest().body(response);
            }

            // Process and save the resume
            Resume savedResume = resumeService.processAndSaveResume(file);

            response.put("status", "success");
            response.put("message", "Resume uploaded successfully");
            response.put("resumeId", savedResume.getId());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return errorResponse("Error reading file: " + e.getMessage());
        } catch (TikaException e) {
            return errorResponse("Error extracting text from file: " + e.getMessage());
        } catch (Exception e) {
            return errorResponse("Unexpected error: " + e.getMessage());
        }
    }

    private boolean isValidFileType(String contentType) {
        return "application/pdf".equals(contentType) ||
               "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        return ResponseEntity.internalServerError().body(error);
    }
}

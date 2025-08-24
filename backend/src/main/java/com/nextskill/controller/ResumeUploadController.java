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
@RequiredArgsConstructor
public class ResumeUploadController {

    private final ResumeService resumeService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return buildErrorResponse("Please select a file to upload.");
        }

        String contentType = file.getContentType();
        if (!isValidFileType(contentType)) {
            return buildErrorResponse("Only PDF and DOCX files are allowed.");
        }

        try {
            Resume savedResume = resumeService.processAndSaveResume(file);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Resume uploaded and parsed successfully.");
            response.put("resumeId", savedResume.getId());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return buildErrorResponse("Error reading the uploaded file: " + e.getMessage());
        } catch (TikaException e) {
            return buildErrorResponse("Error extracting text from the file. It might be corrupted or in an unsupported format: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    private boolean isValidFileType(String contentType) {
        return "application/pdf".equals(contentType) ||
               "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        if (message.contains("unexpected")) {
            return ResponseEntity.internalServerError().body(errorResponse);
        }
        return ResponseEntity.badRequest().body(errorResponse);
    }
}

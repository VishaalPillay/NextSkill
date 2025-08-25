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
// The @CrossOrigin annotation has been removed to rely on the global WebConfig.
@RequiredArgsConstructor
public class ResumeUploadController {

    private final ResumeService resumeService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file) {
        // --- VALIDATION ---
        // 1. Check if the file is empty
        if (file.isEmpty()) {
            return buildErrorResponse("Please select a file to upload.");
        }

        // 2. Check for a valid content type
        String contentType = file.getContentType();
        if (!isValidFileType(contentType)) {
            return buildErrorResponse("Only PDF and DOCX files are allowed.");
        }

        // --- PROCESSING ---
        try {
            // Process and save the resume using the service layer
            Resume savedResume = resumeService.processAndSaveResume(file);

            // Build a successful response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Resume uploaded and parsed successfully.");
            response.put("resumeId", savedResume.getId());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            // Catches errors related to reading the file's input stream
            return buildErrorResponse("Error reading the uploaded file: " + e.getMessage());
        } catch (TikaException e) {
            // Catches errors from the Tika library if it cannot parse the document
            return buildErrorResponse("Error extracting text from the file. It might be corrupted or in an unsupported format: " + e.getMessage());
        } catch (Exception e) {
            // A general catch-all for any other unexpected errors during processing
            e.printStackTrace(); // Logs the full stack trace for debugging
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    private boolean isValidFileType(String contentType) {
        return "application/pdf".equals(contentType) ||
               "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType);
    }

    // Helper method to create a standardized error response
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        // Use 500 for unexpected server errors, 400 for client-side validation errors
        if (message.contains("unexpected")) {
            return ResponseEntity.internalServerError().body(errorResponse);
        }
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
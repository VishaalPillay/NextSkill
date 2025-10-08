package com.nextskill.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.nextskill.service.SecureHttpClient;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class HybridSkillGapService {

    private static final Logger logger = LoggerFactory.getLogger(HybridSkillGapService.class);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${nlp.service.url:http://localhost:5000}")
    private String nlpServiceUrl;

    private final RestTemplate restTemplate;
    private final SecureHttpClient secureHttpClient;
    private final ObjectMapper objectMapper;

    public HybridSkillGapService(SecureHttpClient secureHttpClient) {
        this.restTemplate = new RestTemplate();
        this.secureHttpClient = secureHttpClient;
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> analyzeSkillGap(String jobDescription, List<String> resumeSkills) {
        try {
            // Try Gemini API first
            if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
                return analyzeWithGemini(jobDescription, resumeSkills);
            }
        } catch (RuntimeException e) {
            logger.warn("Gemini API failed: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error with Gemini API", e);
        }

        // Fallback to internal NLP
        return analyzeWithNLP(jobDescription, resumeSkills);
    }

    private Map<String, Object> analyzeWithGemini(String jobDescription, List<String> resumeSkills) throws Exception {
        String prompt = createSkillAnalysisPrompt(jobDescription, resumeSkills);
        
        // Gemini API request
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
        
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", Arrays.asList(part));
        requestBody.put("contents", Arrays.asList(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        // Parse Gemini response with proper error handling
        String responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("Empty response from Gemini API");
        }
        
        JsonNode responseJson = objectMapper.readTree(responseBody);
        JsonNode candidates = responseJson.path("candidates");
        if (candidates.isEmpty()) {
            throw new RuntimeException("No candidates in Gemini response");
        }
        
        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (parts.isEmpty()) {
            throw new RuntimeException("No parts in Gemini response");
        }
        
        String generatedText = parts.get(0).path("text").asText();

        // Extract JSON from response
        return parseSkillAnalysisResponse(generatedText);
    }

    private Map<String, Object> analyzeWithNLP(String jobDescription, List<String> resumeSkills) {
        try {
            // Step 1: Parse job description with NLP
            Map<String, Object> jobParseRequest = new HashMap<>();
            jobParseRequest.put("text", jobDescription);
            
            ResponseEntity<Map> jobParseResponse = secureHttpClient.postToAllowedUrl(
                nlpServiceUrl + "/parse-job", 
                jobParseRequest, 
                Map.class
            );

            // Step 2: Compare skills with NLP
            Map<String, Object> compareRequest = new HashMap<>();
            compareRequest.put("resumeSkills", resumeSkills);
            compareRequest.put("jobSkills", jobParseResponse.getBody().get("skills"));

            ResponseEntity<Map> compareResponse = secureHttpClient.postToAllowedUrl(
                nlpServiceUrl + "/compare-skills",
                compareRequest,
                Map.class
            );

            return compareResponse.getBody();
        } catch (RuntimeException e) {
            logger.error("NLP service failed: {}", e.getMessage());
            throw new RuntimeException("Both Gemini and NLP services failed", e);
        } catch (Exception e) {
            logger.error("Unexpected error with NLP service", e);
            throw new RuntimeException("Both Gemini and NLP services failed", e);
        }
    }

    private String createSkillAnalysisPrompt(String jobDescription, List<String> resumeSkills) {
        return String.format("""
            Analyze this job description and compare it with the candidate's resume skills.
            
            JOB DESCRIPTION:
            %s
            
            CANDIDATE'S RESUME SKILLS:
            %s
            
            Please provide a JSON response with the following structure:
            {
                "requiredSkills": ["skill1", "skill2", ...],
                "matchedSkills": ["matched_skill1", "matched_skill2", ...],
                "missingSkills": ["missing_skill1", "missing_skill2", ...],
                "additionalSkills": ["additional_skill1", "additional_skill2", ...],
                "matchPercentage": 75,
                "totalRequired": 10,
                "totalMatched": 7,
                "jobTitle": "extracted job title",
                "company": "extracted company name"
            }
            
            Rules:
            - Extract all technical skills, tools, frameworks, languages from job description
            - Compare with resume skills using semantic similarity (e.g., "React" matches "ReactJS")
            - Calculate match percentage as (matched skills / required skills) * 100
            - Include soft skills only if explicitly mentioned as requirements
            - Return ONLY the JSON, no additional text
            """, jobDescription, String.join(", ", resumeSkills));
    }

    private Map<String, Object> parseSkillAnalysisResponse(String responseText) throws Exception {
        // Extract JSON from response (handle cases where AI adds extra text)
        String jsonStr = responseText.trim();
        if (jsonStr.startsWith("```json")) {
            jsonStr = jsonStr.substring(7);
        }
        if (jsonStr.endsWith("```")) {
            jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
        }
        
        return objectMapper.readValue(jsonStr, Map.class);
    }
}

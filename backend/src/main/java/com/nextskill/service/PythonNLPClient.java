package com.nextskill.service;

import com.nextskill.dto.PythonNLPResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Collections;
import java.util.Map;

@Service
public class PythonNLPClient {

    private final RestTemplate restTemplate = new RestTemplate();
    // This is the URL of the Python service we created.
    private final String pythonServiceUrl = "http://localhost:5000/parse";

    public PythonNLPResponse parseResumeText(String rawText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create the request body with the raw text
        Map<String, String> requestBody = Collections.singletonMap("text", rawText);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        // Call the Python service and get the parsed data back
        return restTemplate.postForObject(pythonServiceUrl, request, PythonNLPResponse.class);
    }
}

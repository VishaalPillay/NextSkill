package com.nextskill.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

@Service
public class SecureHttpClient {
    
    private static final Set<String> ALLOWED_HOSTS = Set.of("localhost:5000", "127.0.0.1:5000");
    private final RestTemplate restTemplate;
    
    public SecureHttpClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public <T> ResponseEntity<T> postToAllowedUrl(String url, Object request, Class<T> responseType) {
        validateUrl(url);
        return restTemplate.postForEntity(url, request, responseType);
    }
    
    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);
            String hostPort = uri.getHost() + ":" + uri.getPort();
            if (!ALLOWED_HOSTS.contains(hostPort)) {
                throw new SecurityException("URL not allowed: " + url);
            }
        } catch (URISyntaxException e) {
            throw new SecurityException("Invalid URL: " + url);
        }
    }
}
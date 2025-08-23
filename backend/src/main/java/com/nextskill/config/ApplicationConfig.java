package com.nextskill.config;

import com.nextskill.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {
    private final ResumeRepository repository;

    // This Bean is for your future security setup, it's fine to leave it here.
    /*
    @Bean
    public UserDetailsService userDetailsService() {
        // ... your existing code ...
    }
    */

    /**
     * This is the new, correct way to configure CORS globally for your application.
     * It allows your frontend (running on http://localhost:3000) to communicate
     * with your backend, including sending credentials.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Apply to all endpoints in the application
                        .allowedOrigins("http://localhost:3000") // Explicitly allow your frontend's origin
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Specify allowed HTTP methods
                        .allowedHeaders("*") // Allow all headers
                        .allowCredentials(true); // Allow cookies and authentication headers
            }
        };
    }
}
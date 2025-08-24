package com.nextskill.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    @Bean
    @ConfigurationProperties(prefix = "nlp")
    public NlpProperties nlpProperties() {
        return new NlpProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.web.cors")
    public CorsProperties corsProperties() {
        return new CorsProperties();
    }

    @lombok.Data
    public static class NlpProperties {
        private double confidenceThreshold = 0.5;
        private boolean skillDetectionEnabled = true;
        private boolean nameExtractionEnabled = true;
        private boolean contactExtractionEnabled = true;
    }

    @lombok.Data
    public static class CorsProperties {
        private List<String> allowedOrigins;
        private List<String> allowedMethods;
        private List<String> allowedHeaders;
        private boolean allowCredentials = true;
    }
}

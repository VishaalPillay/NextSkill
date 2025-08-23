package com.nextskill.service;

import com.nextskill.model.Resume;
import com.nextskill.model.ResumeSkill;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NLPService {

    private final TokenizerME tokenizer;
    private final NameFinderME nameFinder;

    // Pre-compiled regex for efficiency and robustness
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,7}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+?\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[-. ]?\\d{3}[-. ]?\\d{4}");

    // A comprehensive set of skills for high-accuracy parsing. Using a Set for fast lookups.
    private static final Set<String> SKILL_KEYWORDS = new HashSet<>(Arrays.asList(
            // Programming Languages
            "java", "python", "javascript", "typescript", "c#", "c++", "go", "ruby", "php", "swift", "kotlin", "scala", "rust", "dart", "sql", "html", "css",
            // Frontend Frameworks
            "react", "angular", "vue.js", "next.js", "svelte", "jquery", "bootstrap", "tailwindcss",
            // Backend Frameworks
            "spring boot", "django", "flask", "express.js", "node.js", "ruby on rails", ".net", "laravel",
            // Databases
            "postgresql", "mysql", "mongodb", "redis", "microsoft sql server", "oracle", "sqlite", "cassandra", "elasticsearch",
            // DevOps & Cloud
            "docker", "kubernetes", "aws", "azure", "google cloud", "gcp", "terraform", "ansible", "jenkins", "git", "github", "gitlab", "ci/cd",
            // Data Science & ML
            "pandas", "numpy", "scikit-learn", "tensorflow", "pytorch", "keras", "jupyter", "apache spark",
            // ORM & Other Tools
            "hibernate", "jpa", "maven", "gradle", "kafka", "rabbitmq", "graphql", "rest", "soap",
            // Software & Methodologies
            "agile", "scrum", "jira", "linux", "windows", "macos"
            // Add more skills as needed
    ));

    public NLPService() {
        try {
            this.tokenizer = loadTokenizerModel();
            this.nameFinder = loadNameFinderModel();
        } catch (IOException e) {
            throw new IllegalStateException("FATAL: Could not load NLP models from classpath.", e);
        }
    }

    public Resume parseResumeText(String rawText) {
        Resume resume = new Resume();
        resume.setFullName(findName(rawText));
        resume.setEmail(findEmail(rawText));
        resume.setPhoneNumber(findPhoneNumber(rawText));

        List<String> foundSkills = findSkills(rawText);
        for (String skillName : foundSkills) {
            resume.addSkill(new ResumeSkill(skillName));
        }

        return resume;
    }

    private String findName(String text) {
        // Heuristic: The candidate's name is most likely in the first ~100 words.
        // This improves accuracy by avoiding other names (e.g., references) later in the document.
        String[] initialTokens = tokenizer.tokenize(String.join(" ", Arrays.copyOf(text.split("\\s+"), 100)));
        Span[] nameSpans = nameFinder.find(initialTokens);
        if (nameSpans.length > 0) {
            return String.join(" ", Arrays.copyOfRange(initialTokens, nameSpans[0].getStart(), nameSpans[0].getEnd()));
        }
        return null; // Return null if not found, it's cleaner than "Name not found"
    }

    private String findEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(0) : null;
    }

    private String findPhoneNumber(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(0) : null;
    }

    private List<String> findSkills(String text) {
        String lowercasedText = text.toLowerCase();
        // Use word boundaries (\b) in the regex to match whole words only.
        // This prevents "java" from matching in "javascript".
        return SKILL_KEYWORDS.stream()
                .filter(skill -> {
                    Pattern p = Pattern.compile("\\b" + Pattern.quote(skill) + "\\b");
                    return p.matcher(lowercasedText).find();
                })
                .collect(Collectors.toList());
    }

    // --- Model Loading ---
    private TokenizerME loadTokenizerModel() throws IOException {
        try (InputStream modelIn = new ClassPathResource("nlp-models/en-token.bin").getInputStream()) {
            return new TokenizerME(new TokenizerModel(modelIn));
        }
    }

    private NameFinderME loadNameFinderModel() throws IOException {
        try (InputStream modelIn = new ClassPathResource("nlp-models/en-ner-person.bin").getInputStream()) {
            return new NameFinderME(new TokenNameFinderModel(modelIn));
        }
    }
}
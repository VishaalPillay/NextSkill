package com.nextskill.service;

import com.nextskill.model.*;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NLPService {

    private final TokenizerME tokenizer;
    private final NameFinderME nameFinder;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,7}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+?\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[-. ]?\\d{3}[-. ]?\\d{4}");

    private static final Set<String> SKILL_KEYWORDS = new HashSet<>(Arrays.asList(
            "java", "python", "javascript", "typescript", "c#", "c++", "c", "go", "ruby", "php", "swift", "kotlin", "sql", "html", "css",
            "react", "react.js", "angular", "vue.js", "next.js", "node.js", "spring boot", "django", "flask",
            "postgresql", "mysql", "mongodb", "redis", "docker", "kubernetes", "aws", "azure", "gcp",
            "tensorflow", "pytorch", "opencv", "numpy", "pandas", "hibernate", "jpa", "maven", "git", "github", "linux", "tailwind",
            "data structures", "algorithms", "generative al", "data analysis", "json", "api", "ui/ux"
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

        parseExperience(rawText, resume);
        parseProjects(rawText, resume);
        parseCertifications(rawText, resume);

        return resume;
    }

    private String findName(String text) {
        String[] initialTokens = tokenizer.tokenize(String.join(" ", Arrays.copyOf(text.split("\\s+"), 100)));
        Span[] nameSpans = nameFinder.find(initialTokens);

        return Arrays.stream(nameSpans)
                .map(span -> String.join(" ", Arrays.copyOfRange(initialTokens, span.getStart(), span.getEnd())))
                .filter(name -> name.split(" ").length > 1)
                .findFirst()
                .orElseGet(() -> {
                    if (nameSpans.length > 0) {
                        Span firstSpan = nameSpans[0];
                        return String.join(" ", Arrays.copyOfRange(initialTokens, firstSpan.getStart(), firstSpan.getEnd()));
                    }
                    return null;
                });
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
        return SKILL_KEYWORDS.stream()
                .filter(skill -> {
                    Pattern p = Pattern.compile("\\b" + Pattern.quote(skill) + "\\b", Pattern.CASE_INSENSITIVE);
                    return p.matcher(lowercasedText).find();
                })
                .collect(Collectors.toList());
    }

    private void parseExperience(String text, Resume resume) {
        String experienceSection = getSection(text, "Experience", "Projects|Education|Skills|Certifications|Achievements");
        if (experienceSection == null) return;

        String[] entries = experienceSection.split("\n\n");
        for (String entry : entries) {
            if (entry.trim().isEmpty()) continue;
            String[] lines = entry.trim().split("\n");
            if (lines.length >= 2) {
                Experience exp = new Experience();
                exp.setJobTitle(lines[0].trim());
                exp.setCompanyName(lines[1].trim());
                resume.addExperience(exp);
            }
        }
    }

    private void parseProjects(String text, Resume resume) {
        String projectSection = getSection(text, "Projects", "Skills|Certifications|Achievements|Experience");
        if (projectSection == null) return;

        Pattern projectPattern = Pattern.compile(
                "^([A-Z][A-Za-z0-9 -]+)" +
                "\\n" +
                "(?:.*\\n)?" +
                "((?:.|\\n)+?)" +
                "(?=\\n[A-Z][A-Za-z0-9 -]+|$)",
                Pattern.MULTILINE
        );

        Matcher matcher = projectPattern.matcher(projectSection);

        while (matcher.find()) {
            if (matcher.groupCount() >= 2) {
                Project proj = new Project();
                proj.setProjectName(matcher.group(1).trim());
                proj.setDescription(matcher.group(2).trim());
                resume.addProject(proj);
            }
        }
    }


    private void parseCertifications(String text, Resume resume) {
        String certSection = getSection(text, "CERTIFICATIONS", "Achievements|Projects|Experience");
        if (certSection == null) return;

        String[] lines = certSection.trim().split("\n");
        for (int i = 0; i < lines.length; i += 2) {
            if (i + 1 < lines.length) {
                Certification cert = new Certification();
                cert.setCertificationName(lines[i].trim());
                cert.setIssuingOrganization(lines[i+1].trim());
                resume.addCertification(cert);
            }
        }
    }

    private String getSection(String text, String startKeyword, String endKeywords) {
        Pattern p = Pattern.compile(startKeyword + "(.*?)(?=" + endKeywords + "|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

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
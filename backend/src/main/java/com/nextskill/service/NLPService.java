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
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|Present|Current)[a-z]*\\s*\\d{4}(?:\\s*-\\s*(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|Present|Current)[a-z]*\\s*\\d{4})?");

    private static final Set<String> SKILL_KEYWORDS = new HashSet<>(Arrays.asList(
            "java", "python", "javascript", "typescript", "c#", "c++", "c", "go", "ruby", "php", "swift", "kotlin", "sql", "html", "css",
            "react", "react.js", "angular", "vue.js", "next.js", "node.js", "spring boot", "django", "flask",
            "postgresql", "mysql", "mongodb", "redis", "docker", "kubernetes", "aws", "azure", "gcp",
            "tensorflow", "pytorch", "opencv", "numpy", "pandas", "hibernate", "jpa", "maven", "git", "github", "linux", "tailwind",
            "data structures", "algorithms", "generative ai", "data analysis", "json", "api", "ui/ux"
    ));

    private static final Set<String> COMMON_ISSUERS = new HashSet<>(Arrays.asList(
            "nptel", "coursera", "udemy", "udacity", "linkedin learning", "edx", "nvidia", "oracle", "ibm", "microsoft", "google", "aws"
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
        String cleanedText = rawText.replaceAll("[^\\x00-\\x7F]", "").replace("’", "'");

        Resume resume = new Resume();
        resume.setFullName(findName(cleanedText));
        resume.setEmail(findEmail(cleanedText));
        resume.setPhoneNumber(findPhoneNumber(cleanedText));

        List<String> foundSkills = findSkills(cleanedText);
        for (String skillName : foundSkills) {
            resume.addSkill(new ResumeSkill(skillName));
        }

        parseExperience(cleanedText, resume);
        parseProjects(cleanedText, resume);
        parseCertifications(cleanedText, resume);

        return resume;
    }

    // -------------------- NAME / EMAIL / PHONE --------------------
    private String findName(String text) {
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (EMAIL_PATTERN.matcher(lines[i]).find() && PHONE_PATTERN.matcher(lines[i]).find()) {
                if (i > 0) return lines[i - 1].trim();
            }
        }
        String[] initialTokens = tokenizer.tokenize(String.join(" ", Arrays.copyOf(text.split("\\s+"), 50)));
        Span[] nameSpans = nameFinder.find(initialTokens);
        return (nameSpans.length > 0) ? String.join(" ", Arrays.copyOfRange(initialTokens, nameSpans[0].getStart(), nameSpans[0].getEnd())) : null;
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
                .filter(skill -> Pattern.compile("\\b" + Pattern.quote(skill) + "\\b", Pattern.CASE_INSENSITIVE).matcher(lowercasedText).find())
                .collect(Collectors.toList());
    }

    // -------------------- EXPERIENCE --------------------
    private void parseExperience(String text, Resume resume) {
        String section = getSection(text, "Experience", "Projects|Education|Skills|Certifications|Achievements");
        if (section == null) return;

        String[] blocks = section.split("\\n\\s*\\n");
        for (String block : blocks) {
            String[] lines = block.trim().split("\\n");
            if (lines.length >= 2) {
                Experience exp = new Experience();
                exp.setCompanyName(lines[0].trim());
                exp.setJobTitle(lines[1].trim());

                Matcher dateMatcher = DATE_RANGE_PATTERN.matcher(block);
                if (dateMatcher.find()) {
                    exp.setDateRange(dateMatcher.group(0));
                }
                resume.addExperience(exp);
            }
        }
    }

    // -------------------- PROJECTS --------------------
    private void parseProjects(String text, Resume resume) {
        String section = getSection(text, "Projects", "Skills|Certifications|Achievements|Experience|Education");
        if (section == null) return;

        String[] blocks = section.split("\\n\\s*\\n");
        for (String block : blocks) {
            String[] lines = block.trim().split("\\n");
            if (lines.length == 0) continue;

            Project proj = new Project();

            // Step 1: Extract name + possible date
            String firstLine = lines[0].trim();
            Matcher dateMatcher = DATE_RANGE_PATTERN.matcher(firstLine);

            if (dateMatcher.find()) {
                proj.setProjectName(firstLine.substring(0, dateMatcher.start()).trim());
                proj.setDateRange(dateMatcher.group(0));
            } else {
                proj.setProjectName(firstLine);
            }

            // Step 2: If date is on second line
            if (proj.getDateRange() == null && lines.length > 1) {
                Matcher secondLineMatcher = DATE_RANGE_PATTERN.matcher(lines[1]);
                if (secondLineMatcher.find()) {
                    proj.setDateRange(secondLineMatcher.group(0));
                }
            }

            // Step 3: Description
            String description = Arrays.stream(lines)
                    .skip(1)
                    .filter(l -> proj.getDateRange() == null || !l.contains(proj.getDateRange()))
                    .collect(Collectors.joining(" "))
                    .replaceAll("\\s+", " ")
                    .trim();

            proj.setDescription(description);
            resume.addProject(proj);
        }
    }

    // -------------------- CERTIFICATIONS --------------------
    private void parseCertifications(String text, Resume resume) {
        String section = getSection(text, "CERTIFICATIONS", "Achievements|Projects|Experience|Skills|Education");
        if (section == null) return;

        String[] blocks = section.trim().split("\\n\\s*\\n");
        for (String block : blocks) {
            String[] lines = block.trim().split("\\n");
            if (lines.length == 0) continue;

            Certification cert = new Certification();
            String firstLine = lines[0].trim();

            // Case: Name, Org in one line
            if (firstLine.contains(",")) {
                String[] parts = firstLine.split(",", 2);
                cert.setCertificationName(parts[0].trim());
                cert.setIssuingOrganization(parts[1].trim() + (lines.length > 1 ? ", " + lines[1].trim() : ""));
            } else {
                cert.setCertificationName(firstLine);

                // If second line looks like an org → use it
                if (lines.length > 1) {
                    String candidate = lines[1].trim();
                    if (COMMON_ISSUERS.contains(candidate.toLowerCase()) ||
                            candidate.matches(".*(University|Institute|College|Academy|School).*")) {
                        cert.setIssuingOrganization(candidate);
                    }
                }
            }

            resume.addCertification(cert);
        }
    }

    // -------------------- HELPERS --------------------
    private String getSection(String text, String startKeyword, String endKeywords) {
        Pattern p = Pattern.compile(startKeyword + "(.*?)(?=" + endKeywords + "|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
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

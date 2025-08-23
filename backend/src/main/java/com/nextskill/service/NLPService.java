package com.nextskill.service;

import com.nextskill.dto.ParsedResumeData;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NLP Service for processing resume text and extracting structured information
 * Uses Apache OpenNLP for named entity recognition and custom regex patterns
 */
@Service
@Slf4j
public class NLPService {

    // OpenNLP models
    private TokenizerME tokenizer;
    private NameFinderME personNameFinder;

    // Pre-compiled regex patterns for efficient matching
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(?:\\+?1[-. ]?)?\\(?([0-9]{3})\\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})\\b"
    );

    private static final Pattern YEARS_EXPERIENCE_PATTERN = Pattern.compile(
        "(?i)(?:(\\d+)(?:\\+?))\\s*(?:years?|yrs?)\\s*(?:of\\s*)?(?:experience|exp)", 
        Pattern.CASE_INSENSITIVE
    );

    // Enhanced skill keywords organized by category with more comprehensive coverage
    private static final Map<String, Set<String>> SKILL_CATEGORIES = new HashMap<>();
    
    static {
        // Programming Languages - Expanded list
        SKILL_CATEGORIES.put("Programming Languages", Set.of(
            "java", "python", "javascript", "typescript", "c++", "c#", "php", "ruby", "go", "rust",
            "kotlin", "swift", "scala", "r", "matlab", "perl", "shell", "bash", "powershell",
            "html", "css", "sql", "pl/sql", "assembly", "fortran", "cobol", "pascal", "ada",
            "lisp", "prolog", "haskell", "erlang", "elixir", "clojure", "groovy", "dart", "f#"
        ));
        
        // Frameworks & Libraries - More comprehensive
        SKILL_CATEGORIES.put("Frameworks", Set.of(
            "spring", "spring boot", "hibernate", "react", "angular", "vue", "node.js", "express",
            "django", "flask", "rails", "laravel", ".net", "asp.net", "junit", "mockito", "jest",
            "bootstrap", "tailwind", "material-ui", "ant design", "jquery", "lodash", "underscore",
            "struts", "jsf", "wicket", "vaadin", "gwt", "play framework", "dropwizard", "micronaut",
            "quarkus", "vert.x", "akka", "spark", "tensorflow", "pytorch", "scikit-learn", "pandas",
            "numpy", "matplotlib", "seaborn", "plotly", "d3.js", "chart.js", "socket.io", "graphql"
        ));
        
        // Databases - Expanded
        SKILL_CATEGORIES.put("Databases", Set.of(
            "mysql", "postgresql", "oracle", "mongodb", "redis", "elasticsearch", "cassandra",
            "sqlite", "sql server", "dynamodb", "firebase", "neo4j", "influxdb", "couchdb",
            "mariadb", "db2", "sybase", "hbase", "hive", "impala", "presto", "snowflake",
            "bigquery", "redshift", "aurora", "documentdb", "timestream", "keyspaces"
        ));
        
        // Cloud & DevOps - Comprehensive
        SKILL_CATEGORIES.put("Cloud Platforms", Set.of(
            "aws", "azure", "google cloud", "gcp", "docker", "kubernetes", "jenkins", "gitlab",
            "github actions", "terraform", "ansible", "chef", "puppet", "nagios", "prometheus",
            "ec2", "s3", "lambda", "ecs", "eks", "rds", "dynamodb", "cloudfront", "route53",
            "vpc", "iam", "cloudwatch", "sns", "sqs", "api gateway", "elastic beanstalk",
            "app service", "functions", "cosmos db", "blob storage", "virtual machines",
            "compute engine", "cloud storage", "cloud functions", "bigtable", "datastore",
            "helm", "istio", "linkerd", "consul", "vault", "nomad", "rancher", "openshift",
            "circleci", "travis ci", "bamboo", "teamcity", "gitlab ci", "azure devops"
        ));
        
        // Tools & Technologies - Expanded
        SKILL_CATEGORIES.put("Tools & Technologies", Set.of(
            "git", "maven", "gradle", "npm", "webpack", "babel", "eslint", "sonar", "jira",
            "confluence", "postman", "swagger", "linux", "unix", "windows", "apache", "nginx",
            "tomcat", "jboss", "weblogic", "websphere", "glassfish", "jetty", "undertow",
            "intellij", "eclipse", "vscode", "vim", "emacs", "sublime", "atom", "notepad++",
            "soapui", "insomnia", "curl", "wget", "rsync", "scp", "ssh", "telnet", "ftp",
            "sftp", "ldap", "active directory", "kerberos", "oauth", "jwt", "saml", "openid",
            "oauth2", "oauth 2.0", "rest", "soap", "xml", "json", "yaml", "toml", "ini",
            "csv", "excel", "powerpoint", "word", "outlook", "teams", "slack", "discord",
            "zoom", "webex", "skype", "trello", "asana", "basecamp", "notion", "evernote"
        ));
        
        // Soft Skills - Enhanced
        SKILL_CATEGORIES.put("Soft Skills", Set.of(
            "leadership", "communication", "problem solving", "team work", "project management",
            "analytical thinking", "creativity", "adaptability", "time management", "mentoring",
            "collaboration", "negotiation", "presentation", "public speaking", "critical thinking",
            "decision making", "conflict resolution", "emotional intelligence", "empathy",
            "customer service", "sales", "marketing", "strategic planning", "risk management",
            "quality assurance", "continuous improvement", "agile", "scrum", "kanban", "lean",
            "six sigma", "design thinking", "user experience", "user interface", "accessibility"
        ));
        
        // Data Science & Analytics
        SKILL_CATEGORIES.put("Data Science", Set.of(
            "machine learning", "deep learning", "artificial intelligence", "ai", "ml", "dl",
            "data analysis", "data visualization", "statistics", "probability", "regression",
            "classification", "clustering", "neural networks", "cnn", "rnn", "lstm", "transformer",
            "bert", "gpt", "nlp", "natural language processing", "computer vision", "opencv",
            "image processing", "signal processing", "time series", "forecasting", "optimization",
            "genetic algorithms", "reinforcement learning", "unsupervised learning", "supervised learning"
        ));
        
        // Security & Compliance
        SKILL_CATEGORIES.put("Security", Set.of(
            "cybersecurity", "information security", "network security", "application security",
            "penetration testing", "vulnerability assessment", "security auditing", "compliance",
            "gdpr", "hipaa", "sox", "pci dss", "iso 27001", "nist", "owasp", "encryption",
            "cryptography", "ssl", "tls", "vpn", "firewall", "ids", "ips", "siem", "soc",
            "incident response", "threat hunting", "malware analysis", "forensics", "authentication",
            "authorization", "rbac", "mfa", "2fa", "biometric", "zero trust", "devsecops"
        ));
        
        // Mobile Development
        SKILL_CATEGORIES.put("Mobile Development", Set.of(
            "android", "ios", "react native", "flutter", "xamarin", "ionic", "cordova", "phonegap",
            "swift", "objective-c", "kotlin", "java", "dart", "c#", "xcode", "android studio",
            "app store", "google play", "mobile testing", "ui/ux", "responsive design"
        ));
    }

    /**
     * Initialize OpenNLP models on service startup
     */
    @PostConstruct
    public void initializeModels() {
        try {
            loadTokenizerModel();
            loadNameFinderModel();
            log.info("NLP models loaded successfully");
        } catch (IOException e) {
            log.error("Failed to load NLP models. Using fallback regex-based parsing.", e);
        }
    }

    /**
     * Load tokenizer model for text tokenization
     */
    private void loadTokenizerModel() throws IOException {
        try (InputStream tokenModelStream = new ClassPathResource("nlp-models/en-token.bin").getInputStream()) {
            TokenizerModel tokenModel = new TokenizerModel(tokenModelStream);
            this.tokenizer = new TokenizerME(tokenModel);
        } catch (IOException e) {
            log.warn("Tokenizer model not found, using simple whitespace tokenization");
            // Fallback: we'll use simple string splitting
        }
    }

    /**
     * Load name finder model for person name extraction
     */
    private void loadNameFinderModel() throws IOException {
        try (InputStream nameModelStream = new ClassPathResource("nlp-models/en-ner-person.bin").getInputStream()) {
            TokenNameFinderModel nameModel = new TokenNameFinderModel(nameModelStream);
            this.personNameFinder = new NameFinderME(nameModel);
        } catch (IOException e) {
            log.warn("Person name finder model not found, using regex-based name extraction");
            // Fallback: we'll use heuristics for name extraction
        }
    }

    /**
     * Main method to parse resume text and extract structured data
     * @param resumeText raw text extracted from resume
     * @return ParsedResumeData object with structured information
     */
    public ParsedResumeData parseResumeText(String resumeText) {
        log.info("Starting resume text parsing...");
        
        ParsedResumeData parsedData = new ParsedResumeData();
        
        try {
            // Clean and preprocess the text
            String cleanedText = preprocessText(resumeText);
            
            // Extract different types of information
            parsedData.setFullName(extractPersonName(cleanedText));
            parsedData.setEmail(extractEmail(cleanedText));
            parsedData.setPhoneNumber(extractPhoneNumber(cleanedText));
            parsedData.setSummary(extractSummary(cleanedText));
            parsedData.setYearsOfExperience(extractYearsOfExperience(cleanedText));
            parsedData.setSkills(extractSkills(cleanedText));
            
            // Mark as successful and calculate confidence
            parsedData.setParsingSuccessful();
            
            log.info("Resume parsing completed successfully. Extracted {} skills", 
                    parsedData.getSkills() != null ? parsedData.getSkills().size() : 0);
            
        } catch (Exception e) {
            log.error("Error during resume parsing", e);
            parsedData.setParsingFailed("Parsing failed: " + e.getMessage());
        }
        
        return parsedData;
    }

    /**
     * Preprocess text by cleaning up formatting and normalizing
     */
    private String preprocessText(String text) {
        if (text == null) return "";
        
        return text
            // Remove excessive whitespace
            .replaceAll("\\s+", " ")
            // Remove special characters that might interfere with parsing
            .replaceAll("[\\r\\n\\t]+", " ")
            // Normalize common resume section headers
            .replaceAll("(?i)\\bsummary\\b", "SUMMARY")
            .replaceAll("(?i)\\bexperience\\b", "EXPERIENCE")
            .replaceAll("(?i)\\bskills\\b", "SKILLS")
            .replaceAll("(?i)\\beducation\\b", "EDUCATION")
            .trim();
    }

    /**
     * Extract person's full name using OpenNLP or heuristics
     */
    private String extractPersonName(String text) {
        try {
            if (personNameFinder != null && tokenizer != null) {
                return extractNameWithOpenNLP(text);
            }
        } catch (Exception e) {
            log.warn("OpenNLP name extraction failed, using heuristic approach", e);
        }
        
        // Fallback heuristic approach
        return extractNameWithHeuristics(text);
    }

    /**
     * Extract name using OpenNLP Named Entity Recognition
     */
    private String extractNameWithOpenNLP(String text) {
        String[] tokens = tokenizer.tokenize(text);
        Span[] nameSpans = personNameFinder.find(tokens);
        
        for (Span span : nameSpans) {
            StringBuilder name = new StringBuilder();
            for (int i = span.getStart(); i < span.getEnd(); i++) {
                if (i > span.getStart()) name.append(" ");
                name.append(tokens[i]);
            }
            
            String extractedName = name.toString().trim();
            if (isValidPersonName(extractedName)) {
                return extractedName;
            }
        }
        
        return null;
    }

    /**
     * Extract name using heuristic rules (fallback method)
     */
    private String extractNameWithHeuristics(String text) {
        // Look for name patterns at the beginning of the resume
        String[] lines = text.split("\\n", 10); // Check first 10 lines
        
        for (String line : lines) {
            line = line.trim();
            
            // Skip empty lines or lines with common resume keywords
            if (line.isEmpty() || containsResumeKeywords(line)) continue;
            
            // Check if line looks like a name (2-4 words, mostly letters)
            if (isValidPersonName(line)) {
                return line;
            }
        }
        
        return null;
    }

    /**
     * Check if a string is a valid person name
     */
    private boolean isValidPersonName(String name) {
        if (name == null || name.trim().length() < 2) return false;
        
        String[] words = name.trim().split("\\s+");
        
        // Name should have 2-4 words
        if (words.length < 2 || words.length > 4) return false;
        
        // Each word should be mostly letters and start with capital
        for (String word : words) {
            if (!word.matches("^[A-Z][a-zA-Z'.-]*$")) return false;
        }
        
        return true;
    }

    /**
     * Check if line contains common resume section keywords
     */
    private boolean containsResumeKeywords(String line) {
        String lowerLine = line.toLowerCase();
        return lowerLine.contains("resume") || lowerLine.contains("cv") || 
               lowerLine.contains("curriculum") || lowerLine.contains("@") ||
               lowerLine.matches(".*\\b(phone|email|address|linkedin)\\b.*");
    }

    /**
     * Extract email address using regex
     */
    private String extractEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().toLowerCase();
        }
        return null;
    }

    /**
     * Extract phone number using regex
     */
    private String extractPhoneNumber(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        if (matcher.find()) {
            // Format phone number consistently
            return String.format("(%s) %s-%s", 
                matcher.group(1), matcher.group(2), matcher.group(3));
        }
        return null;
    }

    /**
     * Extract professional summary or objective
     */
    private String extractSummary(String text) {
        // Look for summary section
        Pattern summaryPattern = Pattern.compile(
            "(?i)(?:summary|objective|profile|about|overview)\\s*:?\\s*([^\\n\\r]{50,300})",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = summaryPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // If no explicit summary found, try to extract first meaningful paragraph
        String[] paragraphs = text.split("\\n\\s*\\n");
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.length() > 100 && paragraph.length() < 500 && 
                !containsContactInfo(paragraph) && !containsResumeKeywords(paragraph)) {
                return paragraph;
            }
        }
        
        return null;
    }

    /**
     * Check if text contains contact information
     */
    private boolean containsContactInfo(String text) {
        return EMAIL_PATTERN.matcher(text).find() || PHONE_PATTERN.matcher(text).find();
    }

    /**
     * Extract years of experience using regex
     */
    private Integer extractYearsOfExperience(String text) {
        Matcher matcher = YEARS_EXPERIENCE_PATTERN.matcher(text);
        
        int maxYears = 0;
        while (matcher.find()) {
            try {
                int years = Integer.parseInt(matcher.group(1));
                if (years > maxYears && years <= 50) { // Sanity check
                    maxYears = years;
                }
            } catch (NumberFormatException e) {
                // Ignore invalid numbers
            }
        }
        
        return maxYears > 0 ? maxYears : null;
    }

    /**
     * Extract skills and categorize them
     */
    private List<ParsedResumeData.SkillData> extractSkills(String text) {
        List<ParsedResumeData.SkillData> extractedSkills = new ArrayList<>();
        Set<String> foundSkills = new HashSet<>(); // Avoid duplicates
        
        String lowerText = text.toLowerCase();
        
        // Search for each skill in each category
        for (Map.Entry<String, Set<String>> category : SKILL_CATEGORIES.entrySet()) {
            String categoryName = category.getKey();
            
            for (String skill : category.getValue()) {
                double confidence = findSkillInText(lowerText, skill);
                
                if (confidence > 0.5) { // Only add if confidence is reasonable
                    String normalizedSkill = normalizeSkillName(skill);
                    
                    // Avoid duplicates
                    if (!foundSkills.contains(normalizedSkill)) {
                        foundSkills.add(normalizedSkill);
                        extractedSkills.add(new ParsedResumeData.SkillData(
                            normalizedSkill, categoryName, confidence
                        ));
                    }
                }
            }
        }
        
        return extractedSkills;
    }

    /**
     * Find skill in text and calculate confidence score
     */
    private double findSkillInText(String text, String skill) {
        String lowerText = text.toLowerCase();
        String lowerSkill = skill.toLowerCase();
        
        // Exact word boundary match (highest confidence)
        Pattern exactPattern = Pattern.compile("\\b" + Pattern.quote(lowerSkill) + "\\b");
        if (exactPattern.matcher(lowerText).find()) {
            return 0.95;
        }
        
        // Check for common variations and abbreviations
        Map<String, String[]> variations = new HashMap<>();
        variations.put("javascript", new String[]{"js", "node.js", "nodejs"});
        variations.put("typescript", new String[]{"ts"});
        variations.put("postgresql", new String[]{"postgres"});
        variations.put("spring boot", new String[]{"springboot"});
        variations.put("react", new String[]{"react.js", "reactjs"});
        variations.put("angular", new String[]{"angular.js", "angularjs"});
        variations.put("vue", new String[]{"vue.js", "vuejs"});
        variations.put("node.js", new String[]{"nodejs", "node"});
        variations.put("machine learning", new String[]{"ml"});
        variations.put("artificial intelligence", new String[]{"ai"});
        variations.put("deep learning", new String[]{"dl"});
        variations.put("natural language processing", new String[]{"nlp"});
        variations.put("kubernetes", new String[]{"k8s"});
        variations.put("aws", new String[]{"amazon web services"});
        variations.put("azure", new String[]{"microsoft azure"});
        variations.put("google cloud", new String[]{"gcp"});
        variations.put("mysql", new String[]{"my sql"});
        variations.put("mongodb", new String[]{"mongo"});
        variations.put("elasticsearch", new String[]{"elastic search", "es"});
        variations.put("git", new String[]{"version control"});
        variations.put("jenkins", new String[]{"ci/cd"});
        variations.put("terraform", new String[]{"infrastructure as code"});
        variations.put("jira", new String[]{"atlassian jira"});
        variations.put("confluence", new String[]{"atlassian confluence"});
        variations.put("postman", new String[]{"api testing"});
        variations.put("swagger", new String[]{"openapi"});
        variations.put("linux", new String[]{"unix"});
        variations.put("apache", new String[]{"httpd"});
        variations.put("nginx", new String[]{"web server"});
        variations.put("tomcat", new String[]{"apache tomcat"});
        variations.put("intellij", new String[]{"intellij idea"});
        variations.put("eclipse", new String[]{"eclipse ide"});
        variations.put("vscode", new String[]{"visual studio code"});
        variations.put("maven", new String[]{"apache maven"});
        variations.put("npm", new String[]{"node package manager"});
        variations.put("webpack", new String[]{"bundler"});
        variations.put("babel", new String[]{"transpiler"});
        variations.put("eslint", new String[]{"linting"});
        variations.put("sonar", new String[]{"sonarqube"});
        variations.put("agile", new String[]{"agile methodology"});
        variations.put("scrum", new String[]{"agile scrum"});
        variations.put("kanban", new String[]{"kanban board"});
        variations.put("devops", new String[]{"development operations"});
        variations.put("microservices", new String[]{"microservice architecture"});
        variations.put("rest", new String[]{"restful"});
        variations.put("soap", new String[]{"simple object access protocol"});
        variations.put("graphql", new String[]{"graph ql"});
        variations.put("json", new String[]{"javascript object notation"});
        variations.put("xml", new String[]{"extensible markup language"});
        variations.put("yaml", new String[]{"yaml configuration"});
        variations.put("sql", new String[]{"structured query language"});
        variations.put("nosql", new String[]{"no sql"});
        variations.put("html", new String[]{"html5"});
        variations.put("css", new String[]{"css3"});
        variations.put("bootstrap", new String[]{"twitter bootstrap"});
        variations.put("tailwind", new String[]{"tailwind css"});
        variations.put("material-ui", new String[]{"material ui", "mui"});
        variations.put("ant design", new String[]{"antd"});
        variations.put("jquery", new String[]{"jquery library"});
        variations.put("lodash", new String[]{"javascript utility library"});
        variations.put("underscore", new String[]{"javascript utility library"});
        variations.put("socket.io", new String[]{"websockets"});
        variations.put("oauth", new String[]{"oauth2", "oauth 2.0"});
        variations.put("jwt", new String[]{"json web token"});
        variations.put("ssl", new String[]{"secure sockets layer"});
        variations.put("tls", new String[]{"transport layer security"});
        variations.put("vpn", new String[]{"virtual private network"});
        variations.put("firewall", new String[]{"network security"});
        variations.put("encryption", new String[]{"cryptography"});
        variations.put("authentication", new String[]{"auth"});
        variations.put("authorization", new String[]{"permissions"});
        variations.put("rbac", new String[]{"role-based access control"});
        variations.put("mfa", new String[]{"multi-factor authentication", "2fa"});
        variations.put("gdpr", new String[]{"general data protection regulation"});
        variations.put("hipaa", new String[]{"health insurance portability and accountability act"});
        variations.put("sox", new String[]{"sarbanes-oxley act"});
        variations.put("pci dss", new String[]{"payment card industry data security standard"});
        variations.put("iso 27001", new String[]{"information security management"});
        variations.put("owasp", new String[]{"open web application security project"});
        variations.put("penetration testing", new String[]{"pen testing"});
        variations.put("vulnerability assessment", new String[]{"security assessment"});
        variations.put("incident response", new String[]{"security incident"});
        
        if (variations.containsKey(lowerSkill)) {
            for (String variation : variations.get(lowerSkill)) {
                if (lowerText.contains(variation.toLowerCase())) {
                    return 0.85;
                }
            }
        }
        
        // Context-aware matching (medium confidence)
        // Look for skills in context of technology sections
        String[] contextKeywords = {"skills", "technologies", "tools", "frameworks", "languages", "databases", "platforms"};
        for (String context : contextKeywords) {
            if (lowerText.contains(context) && lowerText.contains(lowerSkill)) {
                // Check if skill appears near context keyword
                int contextIndex = lowerText.indexOf(context);
                int skillIndex = lowerText.indexOf(lowerSkill);
                if (Math.abs(contextIndex - skillIndex) < 200) { // Within 200 characters
                    return 0.8;
                }
            }
        }
        
        // Partial match in compound words (lower confidence)
        if (skill.length() > 4 && lowerText.contains(lowerSkill)) {
            return 0.6;
        }
        
        // Check for skill in bullet points or lists
        if (lowerText.contains("• " + lowerSkill) || lowerText.contains("- " + lowerSkill) || 
            lowerText.contains("* " + lowerSkill) || lowerText.contains("✓ " + lowerSkill)) {
            return 0.75;
        }
        
        return 0.0; // Not found
    }

    /**
     * Normalize skill names for consistency
     */
    private String normalizeSkillName(String skill) {
        // Capitalize first letter of each word
        return Arrays.stream(skill.split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(skill);
    }
}
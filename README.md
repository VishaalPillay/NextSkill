# NextSkill - Advanced Resume Parser

NextSkill is a high-accuracy resume parsing system built with Spring Boot and Java NLP technologies. It extracts structured information from resumes including personal details, skills, experience, and provides confidence scores for parsed data.

## 🚀 Features

### High-Accuracy Resume Parsing
- **Multi-format Support**: PDF and DOCX files
- **Advanced NLP**: Apache OpenNLP for Named Entity Recognition
- **Comprehensive Skill Detection**: 200+ technology skills across 8 categories
- **Confidence Scoring**: Each extracted skill comes with a confidence score
- **Context-Aware Parsing**: Understands skill context in resume sections

### Skill Categories Supported
- **Programming Languages**: Java, Python, JavaScript, TypeScript, C++, C#, PHP, Ruby, Go, Rust, and 20+ more
- **Frameworks & Libraries**: Spring Boot, React, Angular, Vue, Django, Flask, and 30+ more
- **Databases**: MySQL, PostgreSQL, MongoDB, Redis, Elasticsearch, and 15+ more
- **Cloud Platforms**: AWS, Azure, Google Cloud, Docker, Kubernetes, and 40+ services
- **Tools & Technologies**: Git, Maven, Jenkins, Jira, and 50+ tools
- **Soft Skills**: Leadership, Communication, Problem Solving, and 20+ skills
- **Data Science**: Machine Learning, Deep Learning, TensorFlow, PyTorch, and 15+ tools
- **Security**: Cybersecurity, Penetration Testing, Compliance, and 20+ security skills
- **Mobile Development**: Android, iOS, React Native, Flutter, and 10+ mobile skills

### API Endpoints
- `POST /api/resume/upload` - Upload and parse resume
- `GET /api/resume/{id}` - Get complete resume details
- `GET /api/resume/list` - List all parsed resumes
- `GET /api/resume/{id}/skills` - Get skills grouped by category
- `GET /api/resume/{id}/stats` - Get resume statistics and analytics
- `POST /api/resume/{id}/reparse` - Reparse existing resume
- `GET /api/resume/health` - Health check endpoint

## 🛠️ Technology Stack

### Backend
- **Spring Boot 3.2.5** - Main framework
- **Java 17** - Programming language
- **PostgreSQL** - Database
- **Apache Tika** - Text extraction from PDF/DOCX
- **Apache OpenNLP** - Natural Language Processing
- **Flyway** - Database migrations
- **Maven** - Build tool

### Key Dependencies
- `opennlp-tools:2.3.3` - Named Entity Recognition
- `tika-core:2.9.2` - Document text extraction
- `spring-boot-starter-data-jpa` - Database ORM
- `postgresql` - Database driver
- `flyway-core` - Database migrations

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- 10MB free disk space for NLP models

## 🚀 Quick Start

### 1. Database Setup
```sql
CREATE DATABASE nextskill_db;
CREATE USER postgres WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE nextskill_db TO postgres;
```

### 2. Configuration
Copy `application.properties.template` to `application.properties` and update:
```properties
spring.datasource.password=your_password_here
```

### 3. Build and Run
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Test the API
```bash
# Health check
curl http://localhost:8080/api/resume/health

# Upload resume (replace with your file)
curl -X POST -F "file=@resume.pdf" http://localhost:8080/api/resume/upload
```

## 📊 API Usage Examples

### Upload Resume
```bash
curl -X POST \
  -F "file=@resume.pdf" \
  http://localhost:8080/api/resume/upload
```

**Response:**
```json
{
  "status": "success",
  "message": "Resume uploaded and processed successfully",
  "resumeId": 1,
  "fileName": "resume.pdf",
  "parsingStatus": "COMPLETED",
  "parsedData": {
    "fullName": "John Doe",
    "email": "john.doe@email.com",
    "phoneNumber": "(555) 123-4567",
    "summary": "Experienced software engineer...",
    "yearsOfExperience": 5,
    "skillCounts": {
      "Programming Languages": 3,
      "Frameworks": 2,
      "Databases": 1
    },
    "totalSkills": 6
  }
}
```

### Get Resume Details
```bash
curl http://localhost:8080/api/resume/1
```

### Get Skills by Category
```bash
curl http://localhost:8080/api/resume/1/skills
```

**Response:**
```json
{
  "status": "success",
  "resumeId": 1,
  "totalSkills": 6,
  "categories": ["Programming Languages", "Frameworks", "Databases"],
  "skillsByCategory": {
    "Programming Languages": [
      {
        "id": 1,
        "skillName": "Java",
        "category": "Programming Languages",
        "confidenceScore": 0.95,
        "isHighConfidence": true
      }
    ]
  }
}
```

## 🎯 Accuracy Improvements

### Enhanced Skill Detection
- **Exact Word Matching**: 95% confidence for exact skill matches
- **Variation Recognition**: 85% confidence for common abbreviations (JS → JavaScript)
- **Context Awareness**: 80% confidence when skills appear near relevant sections
- **Bullet Point Detection**: 75% confidence for skills in lists
- **Partial Matching**: 60% confidence for compound word matches

### Advanced NLP Features
- **Named Entity Recognition**: Uses OpenNLP for person name extraction
- **Heuristic Fallbacks**: Multiple strategies for name extraction
- **Email/Phone Validation**: Regex-based contact information extraction
- **Experience Calculation**: Intelligent years of experience parsing
- **Summary Extraction**: Context-aware professional summary detection

### Confidence Scoring System
- **High Confidence**: ≥0.7 (reliable extraction)
- **Medium Confidence**: 0.5-0.69 (likely correct)
- **Low Confidence**: <0.5 (requires verification)

## 📁 Project Structure

```
NextSkill/
├── backend/
│   ├── src/main/java/com/nextskill/
│   │   ├── controller/
│   │   │   └── ResumeUploadController.java    # REST API endpoints
│   │   ├── service/
│   │   │   ├── ResumeService.java             # Business logic
│   │   │   └── NLPService.java                # NLP processing
│   │   ├── model/
│   │   │   ├── Resume.java                    # Resume entity
│   │   │   └── ResumeSkill.java               # Skill entity
│   │   ├── repository/
│   │   │   ├── ResumeRepository.java          # Resume data access
│   │   │   └── ResumeSkillRepository.java     # Skill data access
│   │   ├── dto/
│   │   │   └── ParsedResumeData.java          # Data transfer objects
│   │   └── config/
│   │       └── ApplicationConfig.java         # Application configuration
│   ├── src/main/resources/
│   │   ├── application.properties             # Configuration
│   │   ├── db/migration/                      # Database migrations
│   │   └── nlp-models/                        # OpenNLP models
│   └── pom.xml                                # Maven dependencies
└── README.md                                  # This file
```

## 🔧 Configuration Options

### NLP Settings
```properties
nlp.confidence.threshold=0.5
nlp.skill.detection.enabled=true
nlp.name.extraction.enabled=true
nlp.contact.extraction.enabled=true
```

### File Upload Limits
```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### Database Performance
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.jpa.properties.hibernate.jdbc.batch_size=20
```

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Manual Testing
1. Upload a PDF resume
2. Check parsing results
3. Verify skill extraction accuracy
4. Test confidence scores

## 📈 Performance Metrics

- **Text Extraction**: <2 seconds for 10MB files
- **NLP Processing**: <5 seconds for typical resumes
- **Database Operations**: <1 second for queries
- **Memory Usage**: ~512MB for typical operation
- **Concurrent Users**: Supports 50+ simultaneous uploads

## 🔒 Security Features

- **File Type Validation**: Only PDF and DOCX allowed
- **File Size Limits**: 10MB maximum
- **Input Sanitization**: All text inputs are cleaned
- **CORS Configuration**: Configurable cross-origin settings
- **Error Handling**: Graceful error responses without data leakage

## 🚀 Deployment

### Docker Deployment
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Production Configuration
```properties
spring.profiles.active=prod
logging.level.com.nextskill=INFO
spring.jpa.show-sql=false
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For issues and questions:
1. Check the documentation
2. Review existing issues
3. Create a new issue with detailed information

## 🔄 Version History

- **v1.0.0** - Initial release with basic resume parsing
- **v1.1.0** - Enhanced skill detection and confidence scoring
- **v1.2.0** - Added comprehensive skill categories and NLP improvements
- **v1.3.0** - Performance optimizations and security enhancements

---

**NextSkill** - Making resume parsing intelligent and accurate! 🎯

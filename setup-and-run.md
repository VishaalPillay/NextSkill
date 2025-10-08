# NextSkill Setup & Test Guide

## 1. Environment Setup

### Set Environment Variables (Windows)
```cmd
# Set your Gemini API key (optional - system will fallback to NLP service)
set GEMINI_API_KEY=your_actual_gemini_api_key_here

# Set your database password
set DB_PASSWORD=your_postgres_password
```

### Or create .env file in backend folder:
```
GEMINI_API_KEY=your_actual_gemini_api_key_here
DB_PASSWORD=your_postgres_password
NLP_SERVICE_URL=http://localhost:5000
```

## 2. Start Services

### Terminal 1: Start Python NLP Service
```cmd
cd "NextSkill-3\backend\nlp parser"
python app.py
```

### Terminal 2: Start Java Backend
```cmd
cd NextSkill-3\backend
mvn spring-boot:run
```

## 3. Test Complete Flow

1. Open `test-complete-flow.html` in your browser
2. Upload a resume (PDF/DOCX)
3. Add job description (optional)
4. Click "Upload & Analyze"
5. See skills you need to improve!

## Services Status Check

- Python NLP: http://localhost:5000
- Java Backend: http://localhost:8080
- Test Page: Open test-complete-flow.html

## Expected Flow

1. **Upload Resume** → Java backend receives file
2. **Extract Text** → Apache Tika extracts text
3. **Parse with NLP** → Python service extracts skills
4. **Store in DB** → PostgreSQL saves structured data
5. **Analyze Gap** → Compare with job requirements
6. **Show Results** → Display skills to improve
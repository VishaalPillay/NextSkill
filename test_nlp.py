import requests
import json

# Test data - our sample resume
resume_text = """John Smith
Software Engineer

Email: john.smith@email.com
Phone: +1-555-123-4567

EXPERIENCE
Senior Software Engineer at Google
March 2020 - Present
• Developed scalable web applications using React and Node.js
• Implemented microservices architecture with Spring Boot
• Worked with PostgreSQL and MongoDB databases
• Used Docker and Kubernetes for containerization

Software Developer at Microsoft
January 2018 - February 2020
• Built REST APIs using Java and Spring Framework
• Developed frontend applications with JavaScript and Angular
• Worked with AWS cloud services
• Used Git for version control

SKILLS
Programming Languages: Java, Python, JavaScript, TypeScript
Frameworks: Spring Boot, React, Angular, Node.js
Databases: PostgreSQL, MongoDB, MySQL
Cloud: AWS, Docker, Kubernetes
Tools: Git, Maven, Jenkins

EDUCATION
Bachelor of Science in Computer Science
University of California, Berkeley
2014 - 2018"""

# Test job description
job_description = """We are looking for a Senior Full Stack Developer with:
- 3+ years experience with React and Node.js
- Strong knowledge of Python and Django
- Experience with AWS and Docker
- Knowledge of PostgreSQL and MongoDB
- Familiarity with Kubernetes and microservices"""

def test_resume_parsing():
    print("Testing Resume Parsing...")
    try:
        response = requests.post('http://localhost:5000/parse', 
                               json={'text': resume_text})
        if response.status_code == 200:
            result = response.json()
            print("[SUCCESS] Resume parsing successful!")
            print(f"Name: {result.get('fullName', 'Not found')}")
            print(f"Email: {result.get('email', 'Not found')}")
            print(f"Skills found: {len(result.get('skills', []))}")
            return result
        else:
            print(f"[ERROR] Resume parsing failed: {response.status_code}")
            return None
    except Exception as e:
        print(f"❌ Error connecting to NLP service: {e}")
        return None

def test_job_parsing():
    print("\nTesting Job Description Parsing...")
    try:
        response = requests.post('http://localhost:5000/parse-job', 
                               json={'text': job_description})
        if response.status_code == 200:
            result = response.json()
            print("✅ Job parsing successful!")
            print(f"Required skills found: {len(result.get('requiredSkills', []))}")
            return result
        else:
            print(f"❌ Job parsing failed: {response.status_code}")
            return None
    except Exception as e:
        print(f"❌ Error connecting to NLP service: {e}")
        return None

def test_skill_comparison():
    print("\nTesting Skill Gap Analysis...")
    resume_skills = ["Java", "Python", "JavaScript", "React", "Node.js", "Spring Boot", "PostgreSQL", "MongoDB", "AWS", "Docker", "Git"]
    job_skills = ["React", "Node.js", "Python", "Django", "AWS", "Docker", "PostgreSQL", "MongoDB", "Kubernetes"]
    
    try:
        response = requests.post('http://localhost:5000/compare-skills', 
                               json={
                                   'resumeSkills': resume_skills,
                                   'jobSkills': job_skills
                               })
        if response.status_code == 200:
            result = response.json()
            print("✅ Skill comparison successful!")
            print(f"Match percentage: {result.get('matchPercentage', 0)}%")
            print(f"Matched skills: {result.get('matchedSkills', [])}")
            print(f"Missing skills: {result.get('missingSkills', [])}")
            return result
        else:
            print(f"❌ Skill comparison failed: {response.status_code}")
            return None
    except Exception as e:
        print(f"❌ Error connecting to NLP service: {e}")
        return None

if __name__ == "__main__":
    print("Starting NextSkill NLP Service Tests\n")
    
    # Test individual components
    resume_result = test_resume_parsing()
    job_result = test_job_parsing()
    comparison_result = test_skill_comparison()
    
    print("\nTest Summary:")
    print(f"Resume Parsing: {'PASS' if resume_result else 'FAIL'}")
    print(f"Job Parsing: {'PASS' if job_result else 'FAIL'}")
    print(f"Skill Comparison: {'PASS' if comparison_result else 'FAIL'}")
import requests
import json

def test_resume_parsing():
    resume = """
    Jane Smith
    Senior Full Stack Developer
    jane.smith@email.com | (555) 987-6543
    
    SKILLS:
    Java, Spring Boot, React, JavaScript, Python, PostgreSQL, AWS, Docker
    
    EXPERIENCE:
    Senior Developer at TechCorp (2020-Present)
    - Built microservices with Spring Boot
    - Developed React applications
    """
    
    print("Testing resume parsing...")
    try:
        response = requests.post('http://localhost:5000/parse', json={'text': resume})
        if response.status_code == 200:
            data = response.json()
            print("SUCCESS: Resume parsing worked!")
            print("Name:", data.get('fullName', 'Not found'))
            print("Email:", data.get('email', 'Not found'))
            skills = [s['skillName'] for s in data.get('skills', [])]
            print("Skills found:", len(skills))
            print("Skills:", ', '.join(skills))
            return skills
        else:
            print("FAILED: Status", response.status_code)
            print("Response:", response.text)
            return []
    except Exception as e:
        print("ERROR:", str(e))
        print("Make sure NLP service is running on port 5000")
        return []

def test_job_parsing():
    job = """
    Senior Java Developer Position
    
    Requirements:
    - 3+ years Java and Spring Boot experience
    - React and JavaScript knowledge
    - PostgreSQL database skills
    - AWS cloud experience
    - Docker containerization
    - Kubernetes orchestration
    """
    
    print("\nTesting job parsing...")
    try:
        response = requests.post('http://localhost:5000/parse-job', json={'text': job})
        if response.status_code == 200:
            data = response.json()
            skills = [s['skillName'] for s in data.get('requiredSkills', [])]
            print("SUCCESS: Job parsing worked!")
            print("Required skills found:", len(skills))
            print("Required skills:", ', '.join(skills))
            return skills
        else:
            print("FAILED: Status", response.status_code)
            return []
    except Exception as e:
        print("ERROR:", str(e))
        return []

def test_skill_gap(resume_skills, job_skills):
    print("\nTesting skill gap analysis...")
    try:
        response = requests.post('http://localhost:5000/compare-skills', json={
            'resumeSkills': resume_skills,
            'jobSkills': job_skills
        })
        if response.status_code == 200:
            data = response.json()
            print("SUCCESS: Skill gap analysis worked!")
            print("Match percentage:", str(data.get('matchPercentage', 0)) + "%")
            print("Matched skills:", ', '.join(data.get('matchedSkills', [])))
            print("Missing skills:", ', '.join(data.get('missingSkills', [])))
            print("Additional skills:", ', '.join(data.get('additionalSkills', [])))
            return True
        else:
            print("FAILED: Status", response.status_code)
            return False
    except Exception as e:
        print("ERROR:", str(e))
        return False

def test_java_backend():
    print("\nTesting Java backend...")
    test_resume = """
    John Doe
    Software Engineer
    john.doe@example.com
    
    SKILLS: Java, Spring Boot, React, PostgreSQL, AWS
    """
    
    try:
        files = {'file': ('test_resume.txt', test_resume.encode('utf-8'), 'text/plain')}
        response = requests.post('http://localhost:8080/api/resume-old/upload', files=files)
        
        if response.status_code == 200:
            data = response.json()
            print("SUCCESS: Java backend upload worked!")
            print("Resume ID:", data.get('resumeId'))
            print("Status:", data.get('status'))
            return data.get('resumeId')
        else:
            print("FAILED: Status", response.status_code)
            print("Response:", response.text)
            return None
    except Exception as e:
        print("ERROR:", str(e))
        print("Make sure Java backend is running on port 8080")
        return None

if __name__ == "__main__":
    print("NextSkill System Test")
    print("=" * 30)
    
    # Test NLP service
    resume_skills = test_resume_parsing()
    job_skills = test_job_parsing()
    
    if resume_skills and job_skills:
        test_skill_gap(resume_skills, job_skills)
    
    # Test Java backend
    resume_id = test_java_backend()
    
    print("\n" + "=" * 30)
    print("Test Summary:")
    print("NLP Service:", "WORKING" if resume_skills else "FAILED")
    print("Job Parsing:", "WORKING" if job_skills else "FAILED") 
    print("Skill Gap:", "WORKING" if resume_skills and job_skills else "FAILED")
    print("Java Backend:", "WORKING" if resume_id else "FAILED")
    
    if resume_skills and job_skills and resume_id:
        print("\nAll systems working! Your NextSkill application is ready.")
    else:
        print("\nSome services failed. Make sure both services are running:")
        print("- Python NLP: python app.py (port 5000)")
        print("- Java Backend: mvn spring-boot:run (port 8080)")
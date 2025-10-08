#!/usr/bin/env python3
import requests
import json
import time

# Test data
SAMPLE_RESUME = """
John Doe
Senior Software Engineer
Email: john.doe@example.com
Phone: (555) 123-4567

PROFESSIONAL SUMMARY
Experienced software engineer with 5+ years in full-stack development.

SKILLS
Programming Languages: Java, Python, JavaScript, TypeScript, Go
Frameworks: Spring Boot, React, Angular, Django, Flask
Databases: PostgreSQL, MongoDB, MySQL, Redis
Cloud & DevOps: AWS, Docker, Kubernetes, Jenkins
Tools: Git, Maven, Jira

EXPERIENCE
Senior Software Engineer at Tech Corp
Jan 2020 - Present
- Developed microservices using Spring Boot and Java
- Built React frontend applications
- Managed PostgreSQL databases

Software Developer at StartupXYZ  
Jun 2018 - Dec 2019
- Created REST APIs with Python Django
- Implemented CI/CD pipelines with Jenkins
"""

SAMPLE_JOB = """
We are looking for a Senior Full Stack Developer to join our team.

Requirements:
- 3+ years experience with Java and Spring Boot
- Strong knowledge of React and JavaScript
- Experience with PostgreSQL and MongoDB
- AWS cloud experience required
- Docker and Kubernetes knowledge
- Python experience is a plus
- Strong problem-solving skills
"""

def test_nlp_service():
    """Test Python NLP service directly"""
    print("🔍 Testing NLP Service...")
    
    try:
        response = requests.post('http://localhost:5000/parse', 
                               json={'text': SAMPLE_RESUME},
                               timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ NLP Service: SUCCESS")
            print(f"   Name: {result.get('fullName', 'Not detected')}")
            print(f"   Email: {result.get('email', 'Not detected')}")
            print(f"   Phone: {result.get('phoneNumber', 'Not detected')}")
            print(f"   Skills: {len(result.get('skills', []))}")
            
            skills = [s.get('skillName') for s in result.get('skills', [])]
            print(f"   Top skills: {', '.join(skills[:8])}")
            return result
        else:
            print(f"❌ NLP Service failed: {response.status_code}")
            print(f"   Response: {response.text}")
            return None
            
    except Exception as e:
        print(f"❌ NLP Service error: {e}")
        return None

def test_job_parsing():
    """Test job description parsing"""
    print("\n💼 Testing Job Description Parsing...")
    
    try:
        response = requests.post('http://localhost:5000/parse-job',
                               json={'text': SAMPLE_JOB},
                               timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Job Parsing: SUCCESS")
            print(f"   Required skills: {result.get('totalSkills', 0)}")
            
            skills = [s.get('skillName') for s in result.get('requiredSkills', [])]
            print(f"   Skills: {', '.join(skills[:8])}")
            return result
        else:
            print(f"❌ Job Parsing failed: {response.status_code}")
            return None
            
    except Exception as e:
        print(f"❌ Job Parsing error: {e}")
        return None

def test_skill_comparison():
    """Test skill gap analysis"""
    print("\n🎯 Testing Skill Gap Analysis...")
    
    resume_skills = ["Java", "Python", "JavaScript", "React", "Spring Boot", "PostgreSQL", "AWS", "Docker"]
    job_skills = ["Java", "Spring Boot", "React", "JavaScript", "PostgreSQL", "MongoDB", "AWS", "Docker", "Kubernetes", "Python"]
    
    try:
        response = requests.post('http://localhost:5000/compare-skills',
                               json={
                                   'resumeSkills': resume_skills,
                                   'jobSkills': job_skills
                               },
                               timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Skill Comparison: SUCCESS")
            print(f"   Match percentage: {result.get('matchPercentage', 0)}%")
            print(f"   Matched skills: {len(result.get('matchedSkills', []))}")
            print(f"   Missing skills: {len(result.get('missingSkills', []))}")
            print(f"   Matched: {', '.join(result.get('matchedSkills', []))}")
            print(f"   Missing: {', '.join(result.get('missingSkills', []))}")
            return result
        else:
            print(f"❌ Skill Comparison failed: {response.status_code}")
            return None
            
    except Exception as e:
        print(f"❌ Skill Comparison error: {e}")
        return None

def test_java_backend():
    """Test Java backend resume upload"""
    print("\n☕ Testing Java Backend...")
    
    # Create a simple text file to upload
    test_file_content = SAMPLE_RESUME.encode('utf-8')
    
    try:
        files = {'file': ('test_resume.txt', test_file_content, 'text/plain')}
        response = requests.post('http://localhost:8080/api/resume-old/upload',
                               files=files,
                               timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Java Backend: SUCCESS")
            print(f"   Status: {result.get('status')}")
            print(f"   Resume ID: {result.get('resumeId')}")
            return result.get('resumeId')
        else:
            print(f"❌ Java Backend failed: {response.status_code}")
            print(f"   Response: {response.text}")
            return None
            
    except Exception as e:
        print(f"❌ Java Backend error: {e}")
        return None

def test_skill_gap_controller(resume_id):
    """Test skill gap controller"""
    if not resume_id:
        print("\n⚠️  Skipping Skill Gap Controller test (no resume ID)")
        return
        
    print(f"\n🔄 Testing Skill Gap Controller with Resume ID: {resume_id}...")
    
    try:
        response = requests.post(f'http://localhost:8080/api/skill-gap/compare/{resume_id}',
                               json={'text': SAMPLE_JOB},
                               timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            print("✅ Skill Gap Controller: SUCCESS")
            print(f"   Match percentage: {result.get('matchPercentage', 0)}%")
            print(f"   Resume name: {result.get('resumeName', 'Unknown')}")
            print(f"   Total resume skills: {result.get('totalResumeSkills', 0)}")
        else:
            print(f"❌ Skill Gap Controller failed: {response.status_code}")
            print(f"   Response: {response.text}")
            
    except Exception as e:
        print(f"❌ Skill Gap Controller error: {e}")

def main():
    print("🚀 NextSkill Internal Testing Suite")
    print("=" * 50)
    
    # Test individual components
    nlp_result = test_nlp_service()
    job_result = test_job_parsing()
    comparison_result = test_skill_comparison()
    
    # Test Java backend
    resume_id = test_java_backend()
    test_skill_gap_controller(resume_id)
    
    print("\n" + "=" * 50)
    print("📊 Test Summary:")
    print(f"   NLP Service: {'✅' if nlp_result else '❌'}")
    print(f"   Job Parsing: {'✅' if job_result else '❌'}")
    print(f"   Skill Comparison: {'✅' if comparison_result else '❌'}")
    print(f"   Java Backend: {'✅' if resume_id else '❌'}")
    
    if all([nlp_result, job_result, comparison_result, resume_id]):
        print("\n🎉 All tests passed! Your system is working correctly.")
    else:
        print("\n⚠️  Some tests failed. Check the services are running:")
        print("   - Python NLP service: http://localhost:5000")
        print("   - Java backend: http://localhost:8080")

if __name__ == "__main__":
    main()
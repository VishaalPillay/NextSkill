#!/usr/bin/env python3
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
            print(f"✅ Name: {data.get('fullName')}")
            print(f"✅ Email: {data.get('email')}")
            skills = [s['skillName'] for s in data.get('skills', [])]
            print(f"✅ Skills ({len(skills)}): {', '.join(skills)}")
            return skills
        else:
            print(f"❌ Failed: {response.status_code}")
            return []
    except Exception as e:
        print(f"❌ Error: {e}")
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
            print(f"✅ Required skills ({len(skills)}): {', '.join(skills)}")
            return skills
        else:
            print(f"❌ Failed: {response.status_code}")
            return []
    except Exception as e:
        print(f"❌ Error: {e}")
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
            print(f"✅ Match: {data.get('matchPercentage')}%")
            print(f"✅ Matched: {', '.join(data.get('matchedSkills', []))}")
            print(f"⚠️  Missing: {', '.join(data.get('missingSkills', []))}")
            return True
        else:
            print(f"❌ Failed: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

if __name__ == "__main__":
    print("🔍 Quick NextSkill Test")
    print("=" * 30)
    
    resume_skills = test_resume_parsing()
    job_skills = test_job_parsing()
    
    if resume_skills and job_skills:
        test_skill_gap(resume_skills, job_skills)
        print("\n🎉 All tests completed!")
    else:
        print("\n❌ Tests failed - check if NLP service is running on port 5000")
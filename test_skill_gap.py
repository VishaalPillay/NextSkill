import requests
import json

# Test complete skill gap analysis pipeline
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

SKILLS
Programming Languages: Java, Python, JavaScript, TypeScript
Frameworks: Spring Boot, React, Angular, Node.js
Databases: PostgreSQL, MongoDB, MySQL
Cloud: AWS, Docker, Kubernetes"""

job_description = """We are looking for a Senior Full Stack Developer with:
- 3+ years experience with React and Node.js
- Strong knowledge of Python and Django
- Experience with AWS and Docker
- Knowledge of PostgreSQL and MongoDB
- Familiarity with Kubernetes and microservices"""

print("=== COMPLETE SKILL GAP ANALYSIS TEST ===")

# Step 1: Parse Resume
print("\n1. Parsing Resume...")
try:
    resume_response = requests.post('http://localhost:5000/parse', 
                                  json={'text': resume_text})
    if resume_response.status_code == 200:
        resume_data = resume_response.json()
        resume_skills = [skill['skillName'] for skill in resume_data.get('skills', [])]
        print(f"SUCCESS: Found {len(resume_skills)} skills")
        print(f"Resume skills: {resume_skills[:10]}")  # Show first 10
    else:
        print(f"FAILED: Resume parsing - {resume_response.status_code}")
        exit()
except Exception as e:
    print(f"ERROR: Resume parsing - {e}")
    exit()

# Step 2: Parse Job Description
print("\n2. Parsing Job Description...")
try:
    job_response = requests.post('http://localhost:5000/parse-job', 
                               json={'text': job_description})
    if job_response.status_code == 200:
        job_data = job_response.json()
        job_skills = [skill['skillName'] for skill in job_data.get('requiredSkills', [])]
        print(f"SUCCESS: Found {len(job_skills)} required skills")
        print(f"Job skills: {job_skills}")
    else:
        print(f"FAILED: Job parsing - {job_response.status_code}")
        exit()
except Exception as e:
    print(f"ERROR: Job parsing - {e}")
    exit()

# Step 3: Compare Skills
print("\n3. Analyzing Skill Gap...")
try:
    compare_response = requests.post('http://localhost:5000/compare-skills', 
                                   json={
                                       'resumeSkills': resume_skills,
                                       'jobSkills': job_skills
                                   })
    if compare_response.status_code == 200:
        analysis = compare_response.json()
        print("SUCCESS: Skill gap analysis complete!")
        print(f"\nRESULTS:")
        print(f"Match Percentage: {analysis.get('matchPercentage', 0)}%")
        print(f"Total Required: {analysis.get('totalRequired', 0)}")
        print(f"Total Matched: {analysis.get('totalMatched', 0)}")
        print(f"Matched Skills: {analysis.get('matchedSkills', [])}")
        print(f"Missing Skills: {analysis.get('missingSkills', [])}")
        print(f"Additional Skills: {analysis.get('additionalSkills', [])[:5]}")  # First 5
        
        # Recommendation
        match_pct = analysis.get('matchPercentage', 0)
        if match_pct >= 80:
            print("\nRECOMMENDATION: Excellent match! Apply for this job.")
        elif match_pct >= 60:
            print("\nRECOMMENDATION: Good match! Consider learning missing skills.")
        elif match_pct >= 40:
            print("\nRECOMMENDATION: Fair match. Focus on skill development.")
        else:
            print("\nRECOMMENDATION: Significant skill gap. Use as learning roadmap.")
            
    else:
        print(f"FAILED: Skill comparison - {compare_response.status_code}")
        print(compare_response.text)
except Exception as e:
    print(f"ERROR: Skill comparison - {e}")

print("\n=== TEST COMPLETE ===")
import requests
import json

def demo_parsing_capabilities():
    print("NextSkill Parsing & Analysis Demo")
    print("=" * 50)
    
    # Test Case 1: Software Engineer Resume
    resume = """
    Alex Chen
    Full Stack Developer
    alex.chen@gmail.com | (555) 123-4567
    
    SKILLS:
    Java, Spring Boot, React, Node.js, PostgreSQL, AWS, Docker, Kubernetes
    
    EXPERIENCE:
    Full Stack Developer at InnovateX (2021-Present)
    - Built REST APIs with Spring Boot
    - Developed React frontend applications
    - Managed PostgreSQL databases
    """
    
    # Test Case 2: Job Posting
    job = """
    Senior Backend Developer - TechStartup
    
    We need:
    - 3+ years Java experience
    - Spring Boot expertise
    - PostgreSQL knowledge
    - AWS cloud skills
    - Docker experience
    - Microservices architecture
    - Redis caching
    """
    
    print("RESUME ANALYSIS:")
    print("-" * 20)
    
    # Parse resume
    response = requests.post('http://localhost:5000/parse', json={'text': resume})
    if response.status_code == 200:
        resume_data = response.json()
        print(f"Candidate: {resume_data.get('fullName')}")
        print(f"Email: {resume_data.get('email')}")
        print(f"Phone: {resume_data.get('phoneNumber')}")
        
        resume_skills = [s['skillName'] for s in resume_data.get('skills', [])]
        print(f"Skills ({len(resume_skills)}): {', '.join(resume_skills)}")
        
        experiences = resume_data.get('experiences', [])
        if experiences:
            for exp in experiences:
                print(f"Experience: {exp.get('jobTitle')} at {exp.get('companyName')}")
    
    print(f"\nJOB REQUIREMENTS:")
    print("-" * 20)
    
    # Parse job
    response = requests.post('http://localhost:5000/parse-job', json={'text': job})
    if response.status_code == 200:
        job_data = response.json()
        job_skills = [s['skillName'] for s in job_data.get('requiredSkills', [])]
        print(f"Required Skills ({len(job_skills)}): {', '.join(job_skills)}")
    
    print(f"\nSKILL GAP ANALYSIS:")
    print("-" * 20)
    
    # Compare skills
    response = requests.post('http://localhost:5000/compare-skills', json={
        'resumeSkills': resume_skills,
        'jobSkills': job_skills
    })
    
    if response.status_code == 200:
        analysis = response.json()
        match_pct = analysis.get('matchPercentage', 0)
        
        print(f"MATCH SCORE: {match_pct}%")
        print(f"Skills Matched: {analysis.get('totalMatched')}/{analysis.get('totalRequired')}")
        
        print(f"\nMATCHED SKILLS:")
        for skill in analysis.get('matchedSkills', []):
            print(f"  [+] {skill}")
        
        print(f"\nMISSING SKILLS:")
        for skill in analysis.get('missingSkills', []):
            print(f"  [-] {skill}")
        
        print(f"\nADDITIONAL SKILLS:")
        for skill in analysis.get('additionalSkills', []):
            print(f"  [*] {skill}")
        
        # Recommendation
        print(f"\nRECOMMENDATION:")
        if match_pct >= 80:
            print("  EXCELLENT MATCH - Strong candidate!")
        elif match_pct >= 60:
            print("  GOOD MATCH - Consider for interview")
        elif match_pct >= 40:
            print("  FAIR MATCH - Some skill gaps")
        else:
            print("  POOR MATCH - Major skill development needed")
    
    print("\n" + "=" * 50)
    print("Demo completed successfully!")
    print("Your NextSkill system can:")
    print("[+] Extract names, emails, phone numbers")
    print("[+] Identify technical skills accurately") 
    print("[+] Parse work experience with companies")
    print("[+] Analyze job requirements")
    print("[+] Perform intelligent skill matching")
    print("[+] Calculate match percentages")
    print("[+] Provide hiring recommendations")

if __name__ == "__main__":
    demo_parsing_capabilities()
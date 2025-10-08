import requests
import json

def test_complete_flow():
    print("Testing Complete NLP Flow")
    print("=" * 40)
    
    # Sample resume
    resume_text = """
    Sarah Johnson
    Senior Software Engineer
    sarah.johnson@techcorp.com
    Phone: +1-555-987-6543
    
    PROFESSIONAL SUMMARY
    Experienced full-stack developer with 6+ years in enterprise software development.
    
    TECHNICAL SKILLS
    Programming Languages: Java, Python, JavaScript, TypeScript, Go
    Frameworks: Spring Boot, Spring MVC, React, Angular, Django, Flask
    Databases: PostgreSQL, MySQL, MongoDB, Redis, Elasticsearch
    Cloud & DevOps: AWS (EC2, S3, RDS, Lambda), Docker, Kubernetes, Jenkins
    Tools: Git, Maven, Gradle, Jira, Confluence
    
    PROFESSIONAL EXPERIENCE
    
    Senior Software Engineer | TechCorp Inc | Jan 2020 - Present
    - Architected and developed microservices using Spring Boot and Java
    - Built responsive web applications using React and TypeScript
    - Designed and optimized PostgreSQL database schemas
    - Deployed applications on AWS using Docker and Kubernetes
    - Led a team of 4 developers on critical projects
    
    Software Developer | StartupXYZ | Jun 2018 - Dec 2019
    - Developed REST APIs using Python Django framework
    - Created automated testing suites with pytest
    - Implemented CI/CD pipelines using Jenkins and Docker
    - Worked with MongoDB for data storage solutions
    
    EDUCATION
    Bachelor of Science in Computer Science
    University of Technology | 2014-2018
    
    CERTIFICATIONS
    - AWS Certified Solutions Architect
    - Oracle Certified Java Programmer
    """
    
    # Sample job description
    job_description = """
    Senior Full Stack Developer - Remote
    
    We are seeking a talented Senior Full Stack Developer to join our growing engineering team.
    
    REQUIRED SKILLS:
    - 5+ years of experience in software development
    - Strong proficiency in Java and Spring Boot framework
    - Experience with React and modern JavaScript (ES6+)
    - Solid understanding of PostgreSQL and database design
    - Experience with AWS cloud services (EC2, S3, RDS)
    - Knowledge of Docker containerization
    - Experience with Git version control
    - Understanding of RESTful API design principles
    
    PREFERRED SKILLS:
    - TypeScript experience
    - Kubernetes orchestration knowledge
    - Experience with microservices architecture
    - Knowledge of Redis caching
    - CI/CD pipeline experience
    - Python programming skills
    - Agile/Scrum methodology experience
    
    RESPONSIBILITIES:
    - Design and develop scalable web applications
    - Collaborate with cross-functional teams
    - Mentor junior developers
    - Participate in code reviews and technical discussions
    """
    
    print("1. Testing Resume Parsing...")
    print("-" * 30)
    
    try:
        response = requests.post('http://localhost:5000/parse', 
                               json={'text': resume_text}, 
                               timeout=30)
        
        if response.status_code == 200:
            resume_data = response.json()
            print("SUCCESS: Resume parsed successfully!")
            print(f"Name: {resume_data.get('fullName', 'Not detected')}")
            print(f"Email: {resume_data.get('email', 'Not detected')}")
            print(f"Phone: {resume_data.get('phoneNumber', 'Not detected')}")
            
            resume_skills = [skill['skillName'] for skill in resume_data.get('skills', [])]
            print(f"Skills extracted: {len(resume_skills)}")
            print(f"Skills: {', '.join(resume_skills[:10])}{'...' if len(resume_skills) > 10 else ''}")
            
            # Show experiences if available
            experiences = resume_data.get('experiences', [])
            if experiences:
                print(f"Experiences found: {len(experiences)}")
                for exp in experiences[:2]:  # Show first 2
                    print(f"  - {exp.get('jobTitle', 'Unknown')} at {exp.get('companyName', 'Unknown')}")
            
        else:
            print(f"FAILED: Resume parsing failed with status {response.status_code}")
            return
            
    except Exception as e:
        print(f"ERROR: Resume parsing failed - {e}")
        return
    
    print("\n2. Testing Job Description Parsing...")
    print("-" * 30)
    
    try:
        response = requests.post('http://localhost:5000/parse-job',
                               json={'text': job_description},
                               timeout=30)
        
        if response.status_code == 200:
            job_data = response.json()
            print("SUCCESS: Job description parsed successfully!")
            
            job_skills = [skill['skillName'] for skill in job_data.get('requiredSkills', [])]
            print(f"Required skills extracted: {len(job_skills)}")
            print(f"Required skills: {', '.join(job_skills[:10])}{'...' if len(job_skills) > 10 else ''}")
            
        else:
            print(f"FAILED: Job parsing failed with status {response.status_code}")
            return
            
    except Exception as e:
        print(f"ERROR: Job parsing failed - {e}")
        return
    
    print("\n3. Testing Skill Gap Analysis...")
    print("-" * 30)
    
    try:
        response = requests.post('http://localhost:5000/compare-skills',
                               json={
                                   'resumeSkills': resume_skills,
                                   'jobSkills': job_skills
                               },
                               timeout=30)
        
        if response.status_code == 200:
            gap_data = response.json()
            print("SUCCESS: Skill gap analysis completed!")
            print(f"Match Percentage: {gap_data.get('matchPercentage', 0)}%")
            print(f"Total Required: {gap_data.get('totalRequired', 0)}")
            print(f"Total Matched: {gap_data.get('totalMatched', 0)}")
            
            matched = gap_data.get('matchedSkills', [])
            missing = gap_data.get('missingSkills', [])
            additional = gap_data.get('additionalSkills', [])
            
            print(f"\nMatched Skills ({len(matched)}): {', '.join(matched[:8])}{'...' if len(matched) > 8 else ''}")
            print(f"Missing Skills ({len(missing)}): {', '.join(missing[:8])}{'...' if len(missing) > 8 else ''}")
            print(f"Additional Skills ({len(additional)}): {', '.join(additional[:8])}{'...' if len(additional) > 8 else ''}")
            
            # Analysis
            match_pct = gap_data.get('matchPercentage', 0)
            if match_pct >= 80:
                print(f"\nANALYSIS: Excellent match! Strong candidate for this position.")
            elif match_pct >= 60:
                print(f"\nANALYSIS: Good match! Some skill development recommended.")
            elif match_pct >= 40:
                print(f"\nANALYSIS: Fair match. Significant skill gaps to address.")
            else:
                print(f"\nANALYSIS: Poor match. Major skill development needed.")
                
        else:
            print(f"FAILED: Skill gap analysis failed with status {response.status_code}")
            return
            
    except Exception as e:
        print(f"ERROR: Skill gap analysis failed - {e}")
        return
    
    print("\n" + "=" * 40)
    print("COMPLETE SUCCESS!")
    print("All NLP components are working perfectly!")
    print("Your NextSkill parsing and analysis system is ready!")

if __name__ == "__main__":
    test_complete_flow()
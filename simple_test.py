import requests
import json

# Simple test without Unicode
resume_text = """John Smith
Software Engineer
Email: john.smith@email.com
Phone: +1-555-123-4567

SKILLS
Programming Languages: Java, Python, JavaScript, TypeScript
Frameworks: Spring Boot, React, Angular, Node.js
Databases: PostgreSQL, MongoDB, MySQL
Cloud: AWS, Docker, Kubernetes"""

print("Testing NLP Service...")

try:
    response = requests.post('http://localhost:5000/parse', 
                           json={'text': resume_text})
    if response.status_code == 200:
        result = response.json()
        print("SUCCESS: Resume parsing worked!")
        print(f"Name: {result.get('fullName', 'Not found')}")
        print(f"Email: {result.get('email', 'Not found')}")
        print(f"Skills found: {len(result.get('skills', []))}")
        for skill in result.get('skills', [])[:5]:  # Show first 5 skills
            print(f"  - {skill.get('skillName', 'Unknown')}")
    else:
        print(f"FAILED: Status code {response.status_code}")
        print(response.text)
except Exception as e:
    print(f"ERROR: {e}")
    print("Make sure Python NLP service is running on port 5000")
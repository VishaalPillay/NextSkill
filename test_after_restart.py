import requests
import time

print("Testing after Java backend restart...")

# Test Java backend
test_resume = "Jane Smith\nSoftware Developer\njane@email.com\nSkills: Java, Spring Boot, React"

try:
    files = {'file': ('resume.txt', test_resume.encode(), 'text/plain')}
    response = requests.post('http://localhost:8080/api/resume-old/upload', files=files, timeout=15)
    
    print(f"Java Backend Status: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"SUCCESS! Resume ID: {data.get('resumeId')}")
        print("Both services are now working!")
    else:
        print(f"Still failing: {response.text}")
        
except Exception as e:
    print(f"Connection error: {e}")
    print("Make sure Java backend is restarted")
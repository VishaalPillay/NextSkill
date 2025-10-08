import requests

def test_nlp_service():
    print("Testing NLP Service Endpoints:")
    
    # Test resume parsing
    resume_text = "John Doe\njohn@email.com\nSkills: Java, Python, React"
    
    try:
        response = requests.post('http://localhost:5000/parse', 
                               json={'text': resume_text}, 
                               timeout=10)
        print(f"   /parse endpoint: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"   Name: {data.get('fullName')}")
            print(f"   Skills: {len(data.get('skills', []))}")
        else:
            print(f"   Error: {response.text}")
    except Exception as e:
        print(f"   ERROR: {e}")

def test_java_backend():
    print("\nTesting Java Backend:")
    
    # Test with a simple text file
    test_resume = "John Smith\nSoftware Engineer\njohn@email.com\nSkills: Java, Spring Boot"
    
    try:
        files = {'file': ('resume.txt', test_resume.encode(), 'text/plain')}
        response = requests.post('http://localhost:8080/api/resume-old/upload', 
                               files=files, 
                               timeout=15)
        print(f"   Upload endpoint: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"   Success: {data.get('status')}")
            print(f"   Resume ID: {data.get('resumeId')}")
        else:
            print(f"   Error: {response.text}")
            print(f"   Headers: {response.headers}")
    except Exception as e:
        print(f"   ERROR: {e}")

if __name__ == "__main__":
    test_nlp_service()
    test_java_backend()
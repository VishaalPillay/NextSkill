import requests

def test_services():
    print("Testing Services...")
    
    # Test Python NLP service
    print("\n1. Testing Python NLP (port 5000):")
    try:
        response = requests.get('http://localhost:5000/', timeout=5)
        print(f"   Status: {response.status_code}")
    except Exception as e:
        print(f"   ERROR: {e}")
    
    # Test Java backend
    print("\n2. Testing Java Backend (port 8080):")
    try:
        response = requests.get('http://localhost:8080/api/resume/health', timeout=5)
        print(f"   Status: {response.status_code}")
    except Exception as e:
        print(f"   ERROR: {e}")
    
    # Test resume upload endpoint
    print("\n3. Testing Resume Upload:")
    try:
        test_file = "Test resume\nJohn Doe\njohn@email.com\nSkills: Java, Python"
        files = {'file': ('test.txt', test_file.encode(), 'text/plain')}
        response = requests.post('http://localhost:8080/api/resume-old/upload', files=files, timeout=10)
        print(f"   Status: {response.status_code}")
        if response.status_code == 200:
            print(f"   Response: {response.json()}")
        else:
            print(f"   Error: {response.text}")
    except Exception as e:
        print(f"   ERROR: {e}")

if __name__ == "__main__":
    test_services()
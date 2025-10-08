import requests
import json

def test_gemini_api():
    """Test Gemini API key directly"""
    
    api_key = "AIzaSyDPs2L7NRTI-KDs4jCeZMY6IMu0t2PM2SA"
    url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
    
    headers = {
        "Content-Type": "application/json",
        "x-goog-api-key": api_key
    }
    
    data = {
        "contents": [{
            "parts": [{
                "text": "Hello, can you respond with just 'API Working'?"
            }]
        }]
    }
    
    print("Testing Gemini API key...")
    
    try:
        response = requests.post(url, headers=headers, json=data, timeout=10)
        print(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            result = response.json()
            print("SUCCESS: Gemini API key is working!")
            print("Response:", result)
        else:
            print("FAILED: Gemini API key failed")
            print("Status:", response.status_code)
            print("Error:", response.text)
            
    except Exception as e:
        print(f"Connection error: {e}")

def test_java_backend_env():
    """Test if Java backend can read environment variables"""
    print("\nTesting Java backend environment...")
    
    try:
        # Test a simple endpoint to see if backend is running
        response = requests.get("http://localhost:8080/api/resume/health", timeout=5)
        print(f"Java Backend Status: {response.status_code}")
        
        if response.status_code == 404:
            print("SUCCESS: Java backend is running (404 is expected)")
        elif response.status_code == 200:
            print("SUCCESS: Java backend is running")
        else:
            print("WARNING: Java backend responding but with unexpected status")
            
    except Exception as e:
        print(f"ERROR: Java backend not responding: {e}")

if __name__ == "__main__":
    test_gemini_api()
    test_java_backend_env()
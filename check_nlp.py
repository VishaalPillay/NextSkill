import requests

try:
    response = requests.post('http://localhost:5000/parse', json={'text': 'test'}, timeout=5)
    print(f"NLP Service Status: {response.status_code}")
    if response.status_code != 200:
        print("NLP service is not running!")
        print("Start it with: cd 'backend/nlp parser' && python app.py")
except Exception as e:
    print(f"NLP Service ERROR: {e}")
    print("Python NLP service is not running on port 5000")
    print("Start it with: cd 'backend/nlp parser' && python app.py")
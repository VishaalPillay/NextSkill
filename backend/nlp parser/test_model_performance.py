import spacy
import json

def test_model_performance():
    """Test the current model's performance"""
    
    print("🧪 Testing NLP Model Performance")
    print("=" * 40)
    
    try:
        # Load the trained model
        nlp = spacy.load("./custom_nlp_model/model-last")
        print("✅ Model loaded successfully!")
        
        # Test sentences with expected entities
        test_cases = [
            {
                "text": "I am a Senior Python Developer at Google",
                "expected": {
                    "SKILL": ["Python"],
                    "JOB_TITLE": ["Senior Python Developer"],
                    "ORG": ["Google"]
                }
            },
            {
                "text": "DevOps Engineer with AWS and Docker experience",
                "expected": {
                    "JOB_TITLE": ["DevOps Engineer"],
                    "SKILL": ["AWS", "Docker"]
                }
            },
            {
                "text": "Frontend Developer using React and TypeScript",
                "expected": {
                    "JOB_TITLE": ["Frontend Developer"],
                    "SKILL": ["React", "TypeScript"]
                }
            },
            {
                "text": "Data Scientist at Microsoft working with TensorFlow",
                "expected": {
                    "JOB_TITLE": ["Data Scientist"],
                    "ORG": ["Microsoft"],
                    "SKILL": ["TensorFlow"]
                }
            },
            {
                "text": "Full Stack Developer with Node.js and MongoDB",
                "expected": {
                    "JOB_TITLE": ["Full Stack Developer"],
                    "SKILL": ["Node.js", "MongoDB"]
                }
            }
        ]
        
        print(f"\n📊 Testing {len(test_cases)} test cases...")
        print("-" * 60)
        
        total_expected = 0
        total_found = 0
        correct_predictions = 0
        
        for i, test_case in enumerate(test_cases, 1):
            text = test_case["text"]
            expected = test_case["expected"]
            
            # Process with model
            doc = nlp(text)
            found_entities = {}
            
            for ent in doc.ents:
                if ent.label_ not in found_entities:
                    found_entities[ent.label_] = []
                found_entities[ent.label_].append(ent.text)
            
            print(f"\n{i}. Text: '{text}'")
            print("   Expected vs Found:")
            
            # Check each entity type
            for label, expected_list in expected.items():
                found_list = found_entities.get(label, [])
                
                print(f"   {label}:")
                print(f"     Expected: {expected_list}")
                print(f"     Found:    {found_list}")
                
                # Count matches
                matches = 0
                for expected_entity in expected_list:
                    if expected_entity in found_list:
                        matches += 1
                        correct_predictions += 1
                
                total_expected += len(expected_list)
                total_found += len(found_list)
                
                accuracy = (matches / len(expected_list)) * 100 if expected_list else 0
                print(f"     Accuracy: {accuracy:.1f}% ({matches}/{len(expected_list)})")
        
        # Overall performance
        print("\n" + "=" * 60)
        print("📈 OVERALL PERFORMANCE")
        print("=" * 60)
        
        precision = (correct_predictions / total_found) * 100 if total_found > 0 else 0
        recall = (correct_predictions / total_expected) * 100 if total_expected > 0 else 0
        f1_score = 2 * (precision * recall) / (precision + recall) if (precision + recall) > 0 else 0
        
        print(f"Total Expected Entities: {total_expected}")
        print(f"Total Found Entities:    {total_found}")
        print(f"Correct Predictions:     {correct_predictions}")
        print(f"Precision:               {precision:.1f}%")
        print(f"Recall:                  {recall:.1f}%")
        print(f"F1-Score:                {f1_score:.1f}%")
        
        # Performance rating
        if f1_score >= 90:
            rating = "🎉 EXCELLENT"
        elif f1_score >= 80:
            rating = "✅ GOOD"
        elif f1_score >= 70:
            rating = "⚠️ FAIR"
        else:
            rating = "❌ NEEDS IMPROVEMENT"
        
        print(f"\nOverall Rating: {rating}")
        
        if f1_score < 80:
            print("\n💡 Suggestions for improvement:")
            print("- Add more training examples")
            print("- Include more diverse text patterns")
            print("- Retrain the model with updated data")
        
    except Exception as e:
        print(f"❌ Error testing model: {e}")
        print("Make sure the model is trained and available at ./custom_nlp_model/model-last")

if __name__ == "__main__":
    test_model_performance()

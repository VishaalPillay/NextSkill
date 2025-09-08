import spacy
from spacy.tokens import DocBin
import json

# --- 1. LOAD THE BASE MODEL AND TRAINING DATA ---
# This script is designed to load your training data,
# which should be in the JSONL format (one JSON object per line).
nlp = spacy.load("en_core_web_sm")

TRAIN_DATA = []
try:
    with open("training_data_clean.jsonl", 'r', encoding='utf-8') as f:
        for line in f:
            # We're using json.loads, which is for decoding a string,
            # as each line is a string containing a JSON object.
            TRAIN_DATA.append(json.loads(line))
except FileNotFoundError:
    # This error handler will now work correctly if you forget to rename the file.
    print("Error: 'training_data_clean.jsonl' file not found. Please create this file with the training data.")
    exit()

# --- 2. CONVERT DATA TO SPACY'S BINARY FORMAT ---
# This converts the list of lists into a DocBin, which
# is a binary format optimized for spaCy training.
db = DocBin()
for entry in TRAIN_DATA:
    text = entry["text"]
    annotations = entry["entities"]
    doc = nlp.make_doc(text)
    ents = []
    for start, end, label in annotations:
        span = doc.char_span(start, end, label=label)
        if span is None:
            print(f"Skipping misaligned entity in text: '{text}'")
        else:
            ents.append(span)
    doc.ents = ents
    db.add(doc)
db.to_disk("./training.spacy")

print("\nTraining data has been successfully converted to 'training.spacy'.")
print("Now, please follow these two steps to create the config file and train the model:")
print("1. Create a config file with the following command:")
print("\n   python -m spacy init fill-config --lang en --pipeline ner --optimize efficiency --force base_config.cfg\n")
print("2. Train the model using the config file and the converted data:")
print("\n   python -m spacy train base_config.cfg --output ./custom_nlp_model --paths.train ./training.spacy --paths.dev ./training.spacy\n")
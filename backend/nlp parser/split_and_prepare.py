import json
import random
from pathlib import Path

import spacy
from spacy.tokens import DocBin


def load_jsonl(path: Path):
    data = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            data.append(json.loads(line))
    return data


def to_docbin(examples, nlp):
    docbin = DocBin()
    for entry in examples:
        text = entry["text"]
        entities = entry.get("entities", [])
        doc = nlp.make_doc(text)
        spans = []
        for start, end, label in entities:
            span = doc.char_span(start, end, label=label)
            if span is None:
                # skip misaligned spans but keep going
                continue
            spans.append(span)
        doc.ents = spans
        docbin.add(doc)
    return docbin


def main():
    base_dir = Path(__file__).parent
    src = base_dir / "training_data_clean.jsonl"
    if not src.exists():
        raise FileNotFoundError("training_data_clean.jsonl not found next to this script")

    examples = load_jsonl(src)
    # deterministic shuffle
    random.Random(42).shuffle(examples)

    split_index = int(0.8 * len(examples))
    train_examples = examples[:split_index]
    dev_examples = examples[split_index:]

    print(f"Total: {len(examples)} | Train: {len(train_examples)} | Dev: {len(dev_examples)}")

    # use a simple English tokenizer to build docs; labels are set via spans
    nlp = spacy.load("en_core_web_sm")

    train_db = to_docbin(train_examples, nlp)
    dev_db = to_docbin(dev_examples, nlp)

    train_db.to_disk(base_dir / "training.spacy")
    dev_db.to_disk(base_dir / "dev.spacy")

    print("Wrote training.spacy and dev.spacy")


if __name__ == "__main__":
    main()




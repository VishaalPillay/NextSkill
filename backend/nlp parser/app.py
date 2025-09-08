import os
import re
import json
from pathlib import Path
from typing import List, Set, Dict, Tuple, Any, Optional

import spacy
from spacy.language import Language
from spacy.matcher import Matcher
from spacy.pipeline import EntityRuler
from flask import Flask, request, jsonify

# --------------------
# Model loading (hybrid-friendly)
# --------------------
BASE_DIR = Path(__file__).resolve().parent

# Prefer a spaCy exported "model-best" if present; fall back to top-level dir if it contains a model
CANDIDATE_MODEL_DIRS = [
    BASE_DIR / "custom_nlp_model" / "model-best",
    BASE_DIR / "custom_nlp_model",
]

ML_AVAILABLE = False
HYBRID_USE_ML = os.getenv("HYBRID_USE_ML", "1") != "0"

# Optional patterns directory for EntityRuler JSONL files (e.g., skills.jsonl, titles.jsonl, orgs.jsonl)
PATTERNS_DIR = Path(os.getenv("PATTERNS_DIR", BASE_DIR / "patterns"))


def load_nlp() -> Language:
    """
    Try to load the custom model (model-best preferred), fall back to en_core_web_sm,
    then finally to a blank English pipeline. Track ML availability.
    """
    global ML_AVAILABLE
    for model_path in CANDIDATE_MODEL_DIRS:
        try:
            if model_path.exists():
                nlp_model = spacy.load(str(model_path))
                ML_AVAILABLE = True
                return nlp_model
        except Exception:
            # try next candidate
            pass

    # Try lightweight default model
    try:
        nlp_model = spacy.load("en_core_web_sm")
        ML_AVAILABLE = False  # using default model; treat ML as unavailable
        return nlp_model
    except Exception:
        # Last-resort blank pipeline
        ML_AVAILABLE = False
        return spacy.blank("en")


nlp = load_nlp()


def try_add_entity_ruler(nlp: Language) -> Optional[EntityRuler]:
    """
    Insert an EntityRuler configured from any available JSONL pattern files.
    This is used to augment SKILL / JOB_TITLE / ORG recognition and improve context-aware matching.
    Fails gracefully if pattern files are not present.
    """
    try:
        # Ensure we don't add multiple rulers on hot-reload
        if "entity_ruler" in nlp.pipe_names:
            return nlp.get_pipe("entity_ruler")

        ruler = nlp.add_pipe("entity_ruler", before="ner" if "ner" in nlp.pipe_names else None)
        pattern_files: List[Path] = []

        # Preferred filenames
        preferred = [
            PATTERNS_DIR / "skills.jsonl",
            PATTERNS_DIR / "titles.jsonl",
            PATTERNS_DIR / "orgs.jsonl",
        ]
        for p in preferred:
            if p.exists():
                pattern_files.append(p)

        # Also load any *.jsonl under PATTERNS_DIR as a fallback (but avoid known training corpora)
        if PATTERNS_DIR.exists():
            for p in PATTERNS_DIR.glob("*.jsonl"):
                if p not in pattern_files and "training" not in p.name.lower():
                    pattern_files.append(p)

        # Backwards fallback: allow top-level *.jsonl that look like ruler patterns
        for p in BASE_DIR.glob("*.jsonl"):
            name_lower = p.name.lower()
            if "training" in name_lower:
                continue
            # Heuristic: if small file or name hints at rules, include
            try:
                if p.stat().st_size < 5_000_000 and any(k in name_lower for k in ["skill", "title", "org", "ruler", "pattern"]):
                    if p not in pattern_files:
                        pattern_files.append(p)
            except Exception:
                pass

        # Load patterns if found
        loaded_any = False
        for pf in pattern_files:
            try:
                with pf.open("r", encoding="utf-8") as f:
                    # Expect JSONL: one JSON per line with {"label": "...", "pattern": ...}
                    patterns = [json.loads(line) for line in f if line.strip()]
                    if patterns:
                        ruler.add_patterns(patterns)
                        loaded_any = True
            except Exception:
                # Ignore malformed files; continue
                continue

        if not loaded_any:
            # If nothing loaded, remove the empty ruler to avoid confusion
            try:
                nlp.remove_pipe("entity_ruler")
            except Exception:
                pass
            return None

        return ruler
    except Exception:
        # If anything goes wrong, don't break the service
        return None


ENTITY_RULER = try_add_entity_ruler(nlp)

# --------------------
# Curated dictionaries / taxonomies
# Note: Extend these lists safely anytime. Keep canonical names in values.
# --------------------
SKILL_ALIASES: Dict[str, str] = {
    # alias -> canonical
    "js": "JavaScript",
    "ts": "TypeScript",
    "py": "Python",
    "nodejs": "Node.js",
    "reactjs": "React",
    "react.js": "React",
    "node": "Node.js",
    "postgres": "PostgreSQL",
    "aws": "AWS",
    "gcp": "Google Cloud",
    "ms sql": "SQL Server",
    "sql server": "SQL Server",
}

SKILL_TAXONOMY: Dict[str, Set[str]] = {
    "Programming Languages": {
        "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "Go", "Ruby", "PHP", "Rust", "Kotlin"
    },
    "Frameworks & Libraries": {
        "Spring Boot", "Spring", "React", "Angular", "Vue", "Django", "Flask", "Express", "FastAPI", "Hibernate", ".NET"
    },
    "Databases": {
        "PostgreSQL", "MySQL", "MongoDB", "Redis", "Elasticsearch", "SQLite", "Oracle", "SQL Server"
    },
    "Cloud & DevOps": {
        "AWS", "Azure", "Google Cloud", "GCP", "Docker", "Kubernetes", "Terraform", "Jenkins", "GitHub Actions"
    },
    "Data Science": {
        "TensorFlow", "PyTorch", "scikit-learn", "NumPy", "Pandas"
    },
    "Tools": {
        "Git", "Maven", "Gradle", "Jira"
    }
}

JOB_TITLES: Set[str] = {
    "Software Engineer", "Senior Software Engineer", "Backend Developer", "Frontend Developer",
    "Full Stack Developer", "Data Scientist", "Machine Learning Engineer", "DevOps Engineer",
    "SRE", "QA Engineer", "Android Developer", "iOS Developer"
}

ORG_SUFFIXES: List[str] = [
    "Inc", "Ltd", "LLC", "Pvt", "Technologies", "Labs", "Systems", "Solutions", "Corp", "Company"
]

# These tokens should never be treated as skills even if matched by generic patterns
SKILL_STOPWORDS: Set[str] = {"intern", "junior", "senior", "fresher", "trainee", "lead", "manager", "associate"}

# --------------------
# Helpers
# --------------------
def normalize_text(text: str) -> str:
    # Normalize to lowercase and separate non-alnum with spaces for safe word-boundary scans
    lowered = text.lower()
    # keep +,.,# for tech terms like c++, .net, node.js
    return " " + re.sub(r"[^a-z0-9+.#]+", " ", lowered) + " "


def canonical_skill(token: str) -> str:
    t = token.strip()
    key = t.lower()
    if key in SKILL_ALIASES:
        return SKILL_ALIASES[key]
    # Normalize common variants
    if key in {"node", "nodejs"}:
        return "Node.js"
    if key in {"reactjs", "react.js"}:
        return "React"
    if key == "postgres":
        return "PostgreSQL"
    # Title-case for typical names, but preserve casing of known acronyms
    if t.upper() in {"AWS", "GCP"}:
        return t.upper()
    if t.lower() in {"c++", "c#", ".net"}:
        return t
    return t[:1].upper() + t[1:]


def extract_email_spacy(doc) -> str:
    for token in doc:
        if getattr(token, "like_email", False):
            return token.text
    return ""


def extract_email_regex(text: str) -> str:
    m = re.search(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}", text)
    return m.group(0) if m else ""


def extract_phone_number(text: str) -> str:
    # Flexible: matches +country, spaces, dashes, parentheses
    phone_re = re.compile(
        r"(?:(?:\+?\d{1,3}[\s-]?)?(?:\(?\d{3}\)?[\s-]?)?\d{3}[\s-]?\d{4})"
    )
    m = phone_re.search(text)
    if not m:
        # Fallback: Mobile: 10 digits
        m = re.search(r"Mobile[:\s]*(\d{10})", text, flags=re.IGNORECASE)
        if m:
            return m.group(1)
        return ""
    # clean: keep digits and leading +
    raw = m.group(0)
    cleaned = re.sub(r"[^\d+]", "", raw)
    return cleaned


def extract_name_ml(doc) -> str:
    for ent in getattr(doc, "ents", []):
        if ent.label_ == "PERSON":
            return ent.text
    return ""


def extract_name_fallback(text: str) -> str:
    # Try "Name: Jane Doe" style
    for line in text.splitlines()[:10]:
        m = re.search(r"^\s*(?:name)\s*[:\-]\s*(.+)$", line.strip(), flags=re.IGNORECASE)
        if m:
            return m.group(1).strip()
    # Heuristic: first line with two capitalized words (avoid ALLCAPS / 1-word)
    for line in text.splitlines()[:10]:
        words = [w for w in re.findall(r"[A-Za-z]+", line)]
        if len(words) >= 2 and words[0][0].isupper() and words[1][0].isupper():
            candidate = f"{words[0]} {words[1]}"
            if 3 <= len(words[0]) <= 20 and 3 <= len(words[1]) <= 20:
                return candidate
    return ""


def extract_skills_dict(text: str) -> Set[str]:
    norm = normalize_text(text)
    found: Set[str] = set()
    # direct category keywords
    for _cat, keywords in SKILL_TAXONOMY.items():
        for kw in keywords:
            k = kw.lower()
            # word-boundary-ish search on normalized text
            if re.search(rf"\b{re.escape(k)}\b", norm):
                found.add(canonical_skill(kw))
    # aliases
    for alias, canonical in SKILL_ALIASES.items():
        if re.search(rf"\b{re.escape(alias)}\b", norm):
            found.add(canonical_skill(canonical))
    # filter out noisy words
    found = {s for s in found if s.lower() not in SKILL_STOPWORDS}
    return found


def extract_job_titles_dict(text: str) -> Set[str]:
    norm = normalize_text(text)
    found: Set[str] = set()
    for title in JOB_TITLES:
        t = title.lower()
        if re.search(rf"\b{re.escape(t)}\b", norm):
            found.add(title)
    return found


def extract_orgs_dict(text: str) -> Set[str]:
    found: Set[str] = set()
    # Match phrases ending with a known suffix, e.g., "Acme Technologies"
    suffix_pattern = r"(?:\b[A-Z][A-Za-z0-9&.,\- ]+?)\s+(?:%s)\b" % "|".join(map(re.escape, ORG_SUFFIXES))
    for m in re.finditer(suffix_pattern, text):
        found.add(m.group(0).strip())
    return found


def extract_entities_ml(doc) -> Tuple[Set[str], Set[str], Set[str]]:
    skills, titles, orgs = set(), set(), set()
    for ent in getattr(doc, "ents", []):
        if ent.label_ == "SKILL":
            s = canonical_skill(ent.text)
            if s.lower() not in SKILL_STOPWORDS:
                skills.add(s)
        elif ent.label_ == "JOB_TITLE":
            titles.add(ent.text)
        elif ent.label_ in {"ORG", "ORGANIZATION"}:
            orgs.add(ent.text)
    return skills, titles, orgs


def merge_sets(*sets: Set[str]) -> List[str]:
    merged = set()
    for s in sets:
        for item in s:
            merged.add(item.strip())
    # sort for stable output
    return sorted(list(merged), key=lambda x: x.lower())


# --------------------
# Sectioning
# --------------------
SECTION_SYNONYMS: Dict[str, List[str]] = {
    "experience": [
        "experience", "work experience", "professional experience", "employment history",
        "career history", "work history", "employment", "work profile"
    ],
    "education": [
        "education", "educational background", "academic background", "qualifications", "academics"
    ],
    "projects": [
        "projects", "project experience", "personal projects", "academic projects"
    ],
    "skills": [
        "skills", "technical skills", "skills & tools", "skills and tools", "technologies",
        "tech stack", "core competencies", "key skills"
    ],
    "certifications": [
        "certifications", "certificates", "licenses", "licences"
    ],
    "summary": [
        "summary", "professional summary", "profile", "about me", "objective"
    ],
}

# Pre-compile a heading regex that matches entire lines equal to any known synonym, case-insensitive.
# We only treat lines that look like headings: mostly alphabetic, possibly spaces, ampersand, slash, plus.
CANONICAL_BY_SYNONYM: Dict[str, str] = {}
for canon, syns in SECTION_SYNONYMS.items():
    for s in syns:
        CANONICAL_BY_SYNONYM[s.lower()] = canon

# Build a pattern that matches any synonym at line level
HEADING_WORDS = sorted(CANONICAL_BY_SYNONYM.keys(), key=len, reverse=True)
HEADING_PATTERN = re.compile(
    r"^\s*(?P<head>" + "|".join(re.escape(h) for h in HEADING_WORDS) + r")\s*:?\s*$",
    flags=re.IGNORECASE | re.MULTILINE,
)


def split_resume_sections(text: str) -> Dict[str, str]:
    """
    Split resume text into canonical sections. Robust to common heading variants.
    Returns a dict: { 'experience': '...', 'education': '...', ... }
    """
    if not text:
        return {}

    # Find heading line spans
    matches = list(HEADING_PATTERN.finditer(text))
    if not matches:
        # No explicit headings; put everything into 'summary'
        return {"summary": text.strip()}

    # Build list of (start_index_of_content, end_index_of_content, canonical_key)
    sections: Dict[str, str] = {}
    for idx, m in enumerate(matches):
        raw_head = m.group("head").strip().lower()
        canon = CANONICAL_BY_SYNONYM.get(raw_head, raw_head)
        start = m.end()  # content starts after the heading line
        end = matches[idx + 1].start() if idx + 1 < len(matches) else len(text)
        content = text[start:end].strip()
        # Merge duplicate headings of same canonical type
        if content:
            prev = sections.get(canon, "")
            sections[canon] = (prev + "\n\n" + content).strip() if prev else content

    return sections


# --------------------
# Experience extraction
# --------------------
DATE_RANGE_REGEX = re.compile(
    r"(?P<start>(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*\s+\d{4}|\d{4})\s*"
    r"(?:-|–|—|to|through|until)\s*"
    r"(?P<end>(?:Present|Current|Now|Till date|Until now|"
    r"(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*\s+\d{4}|\d{4}))",
    flags=re.IGNORECASE,
)


def clean_line(line: str) -> str:
    # Remove bullets and excessive whitespace/delimiters around
    line = re.sub(r"^[\-\u2022\u2023\u25E6\u2043\u2219\*]+\s*", "", line)
    return re.sub(r"\s+", " ", line).strip()


def build_experience_matcher(nlp: Language) -> Matcher:
    """
    Build a spaCy Matcher that uses entity labels (from EntityRuler/NER) to capture patterns like:
      JOB_TITLE at ORG
      JOB_TITLE - ORG
      ORG - JOB_TITLE
    """
    matcher = Matcher(nlp.vocab)

    # JOB_TITLE (at|@|with|-) ORG
    matcher.add(
        "TITLE_AT_ORG",
        [
            [
                {"ENT_TYPE": "JOB_TITLE", "OP": "+"},
                {"LOWER": {"IN": ["at", "@", "with", "-", "–", "—"]}},
                {"ENT_TYPE": {"IN": ["ORG", "ORGANIZATION"]}, "OP": "+"},
            ]
        ],
    )

    # ORG ( - | , | @ ) JOB_TITLE
    matcher.add(
        "ORG_TITLE",
        [
            [
                {"ENT_TYPE": {"IN": ["ORG", "ORGANIZATION"]}, "OP": "+"},
                {"IS_PUNCT": True, "OP": "?"},
                {"LOWER": {"IN": ["-", "–", "—", "@", "as", ":", ","]}, "OP": "?"},
                {"ENT_TYPE": "JOB_TITLE", "OP": "+"},
            ]
        ],
    )

    # Plain JOB_TITLE followed by comma and ORG
    matcher.add(
        "TITLE_COMMA_ORG",
        [
            [
                {"ENT_TYPE": "JOB_TITLE", "OP": "+"},
                {"IS_PUNCT": True, "OP": "?"},
                {"LOWER": { "IN": [",", "-", "–", "—"]}, "OP": "?"},
                {"ENT_TYPE": {"IN": ["ORG", "ORGANIZATION"]}, "OP": "+"},
            ]
        ],
    )

    return matcher


EXPERIENCE_MATCHER = build_experience_matcher(nlp)


def pick_longest(spans: List[spacy.tokens.Span]) -> Optional[str]:
    if not spans:
        return None
    best = max(spans, key=lambda s: len(s.text))
    return best.text.strip()


def detect_date_range(text: str, doc) -> str:
    # Prefer explicit date range regex
    m = DATE_RANGE_REGEX.search(text)
    if m:
        start = m.group("start")
        end = m.group("end")
        return f"{start} - {end}"

    # Fallback to combining DATE entities if present near each other
    dates = [ent.text for ent in getattr(doc, "ents", []) if ent.label_ == "DATE"]
    if len(dates) >= 2:
        # Use first two
        return f"{dates[0]} - {dates[1]}"
    elif len(dates) == 1:
        return dates[0]
    return ""


def extract_experience(experience_text: str) -> List[Dict[str, str]]:
    """
    Parse the 'experience' section to extract structured job entries:
      - job_title
      - company_name
      - date_range
    Uses spaCy Matcher plus entities (from EntityRuler/NER) and regex for dates.
    """
    results: List[Dict[str, str]] = []
    if not experience_text:
        return results

    # Split into logical blocks: paragraphs or bullet clusters
    raw_lines = [l for l in experience_text.splitlines()]
    blocks: List[str] = []

    buf: List[str] = []
    for raw in raw_lines:
        line = raw.strip()
        if not line:
            # empty line ends a block
            if buf:
                blocks.append(" ".join(clean_line(x) for x in buf if x.strip()))
                buf = []
            continue
        buf.append(line)
    if buf:
        blocks.append(" ".join(clean_line(x) for x in buf if x.strip()))

    # Process each block
    for block in blocks:
        if not block or len(block) < 5:
            continue

        doc = nlp(block)

        # Run matcher to locate title/org together
        title_text: Optional[str] = None
        org_text: Optional[str] = None

        matches = EXPERIENCE_MATCHER(doc)
        for _match_id, start, end in matches:
            span = doc[start:end]
            # pull entities from span
            title_spans = [ent for ent in span.ents if ent.label_ == "JOB_TITLE"]
            org_spans = [ent for ent in span.ents if ent.label_ in {"ORG", "ORGANIZATION"}]
            cand_title = pick_longest(title_spans)
            cand_org = pick_longest(org_spans)
            if cand_title and not title_text:
                title_text = cand_title
            if cand_org and not org_text:
                org_text = cand_org
            if title_text and org_text:
                break

        # Fallback to any entities in the block
        if not title_text:
            block_titles = [ent for ent in doc.ents if ent.label_ == "JOB_TITLE"]
            title_text = pick_longest(block_titles)

        if not org_text:
            block_orgs = [ent for ent in doc.ents if ent.label_ in {"ORG", "ORGANIZATION"}]
            org_text = pick_longest(block_orgs)

        # Extra rule fallback: use curated dictionaries if entities missing
        if not title_text:
            dict_titles = extract_job_titles_dict(block)
            if dict_titles:
                # pick longest
                title_text = sorted(dict_titles, key=lambda x: len(x), reverse=True)[0]
        if not org_text:
            dict_orgs = extract_orgs_dict(block)
            if dict_orgs:
                org_text = sorted(dict_orgs, key=lambda x: len(x), reverse=True)[0]

        # Date range
        date_range = detect_date_range(block, doc)

        # If we have at least a title or org or a date, keep an entry
        if title_text or org_text or date_range:
            results.append(
                {
                    "job_title": title_text or "",
                    "company_name": org_text or "",
                    "date_range": date_range or "",
                }
            )

    # Deduplicate consecutive duplicates and drop empty rows where all fields empty
    deduped: List[Dict[str, str]] = []
    prev: Optional[Dict[str, str]] = None
    for item in results:
        if not any(item.values()):
            continue
        if prev and all(prev.get(k, "").lower() == item.get(k, "").lower() for k in ["job_title", "company_name", "date_range"]):
            continue
        deduped.append(item)
        prev = item

    return deduped


# --------------------
# Flask App
# --------------------
app = Flask(__name__)


@app.route("/parse", methods=["POST"])
def parse_resume():
    """
    Context-aware resume parser:
      - Split resume into sections (experience, education, projects, skills, etc.)
      - Parse experience section into structured entries (job_title, company_name, date_range)
      - Extract skills from entire resume using EntityRuler (if available) + curated dictionaries + ML ents
      - Preserve existing helpers for full name, email, phone
    """
    try:
        data = request.get_json(silent=True) or {}
        text = data.get("text", "")
        if not isinstance(text, str) or not text.strip():
            return jsonify({"error": "Invalid request. 'text' field is required."}), 400

        # Always run through spaCy (may be blank). Use ML outputs only if allowed.
        doc = nlp(text)

        # Sectioning
        sections = split_resume_sections(text)
        experience_text = sections.get("experience", "")

        # Structured experience extraction (returns snake_case keys)
        experiences_struct = extract_experience(experience_text)

        # ML path
        ml_fullname = extract_name_ml(doc) if (HYBRID_USE_ML and ML_AVAILABLE) else ""
        ml_skills, ml_titles, ml_orgs = (set(), set(), set())
        if HYBRID_USE_ML and ML_AVAILABLE:
            ml_skills, ml_titles, ml_orgs = extract_entities_ml(doc)

        # EntityRuler-based skills from entire text (label SKILL)
        ruler_skills: Set[str] = set()
        if ENTITY_RULER is not None:
            try:
                # Recompute doc to ensure ruler applied (if not already)
                doc_r = nlp(text)
                for ent in getattr(doc_r, "ents", []):
                    if ent.label_ == "SKILL":
                        s = canonical_skill(ent.text)
                        if s.lower() not in SKILL_STOPWORDS:
                            ruler_skills.add(s)
            except Exception:
                pass

        # Rule-based path
        rb_fullname = extract_name_fallback(text)
        rb_email = extract_email_regex(text)
        rb_phone = extract_phone_number(text)
        rb_skills = extract_skills_dict(text)
        rb_titles = extract_job_titles_dict(text)
        rb_orgs = extract_orgs_dict(text)

        # Email: prefer spaCy token.like_email if present, else regex
        email = extract_email_spacy(doc) or rb_email

        # Phone: regex only
        phone = rb_phone

        # Name: prefer ML PERSON if non-empty, else fallback
        full_name = ml_fullname or rb_fullname

        # Skills/Titles/Orgs: union ML + rules + ruler
        skills = merge_sets(ml_skills, rb_skills, ruler_skills)
        job_titles = merge_sets(ml_titles, rb_titles)
        organizations = merge_sets(rb_orgs, ml_orgs)

        # Convert experiences to API-friendly camelCase
        experiences_api = [
            {
                "jobTitle": e.get("job_title", ""),
                "companyName": e.get("company_name", ""),
                "dateRange": e.get("date_range", ""),
            }
            for e in experiences_struct
        ]

        response_data = {
            "fullName": full_name,
            "email": email,
            "phoneNumber": phone,
            "skills": [{"skillName": s} for s in skills],
            # legacy/aux fields (not required by Java DTO but useful for inspection)
            "jobTitles": job_titles,
            "organizations": organizations,
            # new context-aware structured experiences
            "experiences": experiences_api,
            "projects": [],
            # optional: echo of sections parsed for debugging or extension
            # "sections": sections,  # keep commented to avoid bloating response
        }
        return jsonify(response_data)

    except Exception as e:
        # Never leak stack traces; return concise error
        return jsonify({"error": "Parsing error", "detail": str(e)}), 500


if __name__ == "__main__":
    port = int(os.getenv("PORT", "5000"))
    debug = os.getenv("FLASK_DEBUG", "1") == "1"
    app.run(port=port, debug=debug)

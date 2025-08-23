-- V2__add_structured_resume_data.sql

-- First, remove the old generic text column from the resume table
ALTER TABLE resume DROP COLUMN extracted_text;

-- Next, add new columns for specific, structured data
ALTER TABLE resume ADD COLUMN full_name VARCHAR(255);
ALTER TABLE resume ADD COLUMN email VARCHAR(255);
ALTER TABLE resume ADD COLUMN phone_number VARCHAR(50);
ALTER TABLE resume ADD COLUMN summary TEXT;

-- Create a new table to store the list of skills for each resume
CREATE TABLE resume_skill (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL REFERENCES resume(id),
    skill_name VARCHAR(255) NOT NULL,
    -- Ensures a skill is not added twice for the same resume
    CONSTRAINT uk_resume_skill UNIQUE (resume_id, skill_name)
);

-- Adding an index for faster lookups of skills by resume
CREATE INDEX idx_resume_skill_resume_id ON resume_skill(resume_id);
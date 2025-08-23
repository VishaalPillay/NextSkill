-- V2__add_parsed_fields.sql
-- This migration adds new fields to store parsed resume data

-- Add new columns to existing resume table for structured data
ALTER TABLE resume 
ADD COLUMN full_name VARCHAR(255),
ADD COLUMN email VARCHAR(255),
ADD COLUMN phone_number VARCHAR(50),
ADD COLUMN summary TEXT,
ADD COLUMN years_of_experience INTEGER DEFAULT 0,
ADD COLUMN parsing_status VARCHAR(50) DEFAULT 'PENDING',
ADD COLUMN parsing_error TEXT;

-- Create separate table for skills (many-to-many relationship)
CREATE TABLE IF NOT EXISTS resume_skill (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL,
    skill_name VARCHAR(255) NOT NULL,
    skill_category VARCHAR(100), -- e.g., 'programming', 'database', 'framework'
    confidence_score DECIMAL(3,2), -- 0.00 to 1.00 confidence from NLP
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    
    -- Foreign key constraint
    CONSTRAINT fk_resume_skill_resume 
        FOREIGN KEY (resume_id) 
        REFERENCES resume(id) 
        ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate skills for same resume
    CONSTRAINT uk_resume_skill_unique 
        UNIQUE (resume_id, skill_name)
);

-- Create indexes for better query performance
CREATE INDEX idx_resume_email ON resume(email);
CREATE INDEX idx_resume_parsing_status ON resume(parsing_status);
CREATE INDEX idx_resume_skill_resume_id ON resume_skill(resume_id);
CREATE INDEX idx_resume_skill_category ON resume_skill(skill_category);

-- Add some sample skill categories lookup table (optional, for future use)
CREATE TABLE IF NOT EXISTS skill_category (
    id BIGSERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

-- Insert default skill categories
INSERT INTO skill_category (category_name, description) VALUES
('Programming Languages', 'Languages like Java, Python, JavaScript, etc.'),
('Frameworks', 'Development frameworks like Spring, React, Angular, etc.'),
('Databases', 'Database technologies like MySQL, PostgreSQL, MongoDB, etc.'),
('Cloud Platforms', 'Cloud services like AWS, Azure, Google Cloud, etc.'),
('Tools & Technologies', 'Development tools and general technologies'),
('Soft Skills', 'Communication, leadership, problem-solving, etc.')
ON CONFLICT (category_name) DO NOTHING;
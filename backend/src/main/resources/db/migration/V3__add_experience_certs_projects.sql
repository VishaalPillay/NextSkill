-- V3__add_experience_certs_projects.sql

-- Table to store professional experience
CREATE TABLE experience (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL REFERENCES resume(id),
    job_title VARCHAR(255),
    company_name VARCHAR(255),
    start_date VARCHAR(100),
    end_date VARCHAR(100),
    description TEXT
);

-- Table to store certifications
CREATE TABLE certification (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL REFERENCES resume(id),
    certification_name TEXT,
    issuing_organization VARCHAR(255)
);

-- Table to store projects
CREATE TABLE project (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL REFERENCES resume(id),
    project_name VARCHAR(255),
    description TEXT
);

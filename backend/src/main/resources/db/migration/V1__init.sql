-- V1__init.sql
CREATE TABLE IF NOT EXISTS resume (
  id BIGSERIAL PRIMARY KEY,
  original_file_name VARCHAR(512) NOT NULL,
  extracted_text TEXT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

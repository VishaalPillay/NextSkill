-- V4__refine_parsed_data_tables.sql

-- Add a column to store the timeline/date range for each experience entry
ALTER TABLE experience ADD COLUMN date_range VARCHAR(255);

-- Add a column to store the timeline/date range for each project entry
ALTER TABLE project ADD COLUMN date_range VARCHAR(255);
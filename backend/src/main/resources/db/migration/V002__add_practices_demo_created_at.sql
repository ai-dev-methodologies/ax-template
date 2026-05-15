-- Demo migration fixture: ADD COLUMN as a forward-only schema evolution.
-- The earlier V001__create_practices_demo_table.sql is immutable; new columns / tables
-- arrive as V002__, V003__, ... — never as edits to existing files.
ALTER TABLE practices_demo ADD COLUMN created_at TIMESTAMP;

-- Demo migration fixture for PRACTICES-MIGRATION-001 / 002.
-- Once applied to any environment, this file must NEVER be edited — every change to an
-- applied migration breaks Flyway's checksum check and refuses to start the app.
-- New schema changes go in a new V{N+1}__... file. See practices/rules/migration-*.md.
CREATE TABLE practices_demo (
    id   BIGINT      PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

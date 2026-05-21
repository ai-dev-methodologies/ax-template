-- V019__create_tag_categorization.sql
-- Tag-categorization domain schema (R32).
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration that a Flyway-enabled production deployment would execute.
--
-- Trace:
--   TAG-CRUD-001  — slug UNIQUE constraint
--   TAG-CRUD-003  — slug + parent_tag_id are immutable (enforced at JPA layer; SQL allows ALTER but the application code never issues the UPDATE)
--   TAG-CRUD-004  — FK ON DELETE CASCADE removes attachments when the tag row is deleted
--   TAG-ATTACH-001 — (tag_id, entity_type, entity_id) UNIQUE constraint backs idempotent attach

CREATE TABLE IF NOT EXISTS tags (
    id                  UUID         PRIMARY KEY,
    name                VARCHAR(64)  NOT NULL,
    slug                VARCHAR(64)  NOT NULL,
    parent_tag_id       UUID,
    color               VARCHAR(16),
    created_at          TIMESTAMP    NOT NULL,
    created_by_user_id  VARCHAR(255),
    CONSTRAINT uq_tags_slug UNIQUE (slug)
);

CREATE INDEX IF NOT EXISTS ix_tags_parent ON tags(parent_tag_id);
CREATE INDEX IF NOT EXISTS ix_tags_name   ON tags(name);

CREATE TABLE IF NOT EXISTS tag_attachments (
    id                   UUID         PRIMARY KEY,
    tag_id               UUID         NOT NULL,
    entity_type          VARCHAR(64)  NOT NULL,
    entity_id            VARCHAR(255) NOT NULL,
    attached_at          TIMESTAMP    NOT NULL,
    attached_by_user_id  VARCHAR(255),
    CONSTRAINT uq_tag_attachments_tag_entity UNIQUE (tag_id, entity_type, entity_id),
    CONSTRAINT fk_tag_attachments_tag
        FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_tag_attachments_entity
    ON tag_attachments(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS ix_tag_attachments_tag
    ON tag_attachments(tag_id);

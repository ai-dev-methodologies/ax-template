-- V023__create_comments.sql
-- Comment-thread domain schema (R36).
--
-- Trace:
--   COMMENT-CRUD-001 — author_user_id stamped server-side, @Column(updatable=false)
--   COMMENT-CRUD-003 — body NULLABLE so soft-delete can clear it
--   COMMENT-THREAD-002 — ix_comments_entity_created supports flat list by entity ordered by createdAt
--   COMMENT-HISTORY-001 — comment_edits rows fully immutable

CREATE TABLE IF NOT EXISTS comments (
    id                   UUID         PRIMARY KEY,
    author_user_id       VARCHAR(255) NOT NULL,
    entity_type          VARCHAR(64)  NOT NULL,
    entity_id            VARCHAR(255) NOT NULL,
    parent_comment_id    UUID,
    body                 VARCHAR(4000),
    status               VARCHAR(16)  NOT NULL,
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS ix_comments_entity_created
    ON comments(entity_type, entity_id, created_at);
CREATE INDEX IF NOT EXISTS ix_comments_parent
    ON comments(parent_comment_id);
CREATE INDEX IF NOT EXISTS ix_comments_author_status
    ON comments(author_user_id, status);

CREATE TABLE IF NOT EXISTS comment_edits (
    id                  UUID         PRIMARY KEY,
    comment_id          UUID         NOT NULL,
    edited_at           TIMESTAMP    NOT NULL,
    edited_by_user_id   VARCHAR(255) NOT NULL,
    previous_body       VARCHAR(4000)
);

CREATE INDEX IF NOT EXISTS ix_comment_edits_comment_edited
    ON comment_edits(comment_id, edited_at);

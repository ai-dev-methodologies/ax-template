-- V021__create_favorites.sql
-- Favorites/bookmarks domain schema (R34).
--
-- Trace:
--   FAV-CRUD-001   — UNIQUE(user_id, entity_type, entity_id) backs idempotent add
--   FAV-AUTHZ-002/3 — user_id is the scoping key on every finder + delete
--   FAV-QUERY-002  — ix_favorites_entity supports COUNT(*) per (entityType, entityId)

CREATE TABLE IF NOT EXISTS favorites (
    id              UUID         PRIMARY KEY,
    user_id         VARCHAR(255) NOT NULL,
    entity_type     VARCHAR(64)  NOT NULL,
    entity_id       VARCHAR(255) NOT NULL,
    note            VARCHAR(256),
    favorited_at    TIMESTAMP    NOT NULL,
    CONSTRAINT uq_favorites_user_entity UNIQUE (user_id, entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS ix_favorites_user_favorited
    ON favorites(user_id, favorited_at);
CREATE INDEX IF NOT EXISTS ix_favorites_entity
    ON favorites(entity_type, entity_id);

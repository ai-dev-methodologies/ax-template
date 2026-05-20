-- V010__create_search_index_documents.sql
-- Search domain schema (R17). Reference migration for production Postgres
-- deployments.
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration that a Flyway-enabled production deployment would execute.
-- The DDL is intentionally aligned with what JPA derives from
-- com.ax.template.authblueprint.search.SearchIndexDocument so future Flyway
-- adoption is a drop-in.
--
-- Trace:
--   SEARCH-AUTHZ-002  — every read query filters on tenant_id; covered by
--                       ix_search_tenant_domain and ix_search_tenant_created
--   SEARCH-INDEX-001  — INSERT path persists the document
--   SEARCH-INDEX-002  — DELETE path removes by id within tenant scope
--   SEARCH-QUERY-001  — case-insensitive LIKE on content; manifest documents
--                       the upgrade to tsvector + GIN for Postgres production
--   SEARCH-BACKEND-001 — table is the storage layer for the postgres-fts
--                       default adapter; Meilisearch opt-in skips this table

CREATE TABLE IF NOT EXISTS search_index_documents (
    id          UUID         PRIMARY KEY,
    tenant_id   VARCHAR(255) NOT NULL,
    domain      VARCHAR(64),
    content     VARCHAR(4000) NOT NULL,
    metadata    VARCHAR(4000),
    indexed_at  TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_search_tenant_domain
    ON search_index_documents(tenant_id, domain);

CREATE INDEX IF NOT EXISTS ix_search_tenant_created
    ON search_index_documents(tenant_id, indexed_at);

-- Production upgrade (Postgres FTS):
--   ALTER TABLE search_index_documents
--     ADD COLUMN content_tsv tsvector
--     GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED;
--   CREATE INDEX ix_search_content_tsv ON search_index_documents
--     USING GIN (content_tsv);
--
-- See blueprints/search-manifest.yaml#backend for the opt-in Meilisearch path.

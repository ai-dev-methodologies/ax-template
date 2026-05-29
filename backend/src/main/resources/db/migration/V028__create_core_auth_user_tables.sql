-- IMW1-E (IDW1 entity_migration_guard debt closure, 2026-05-29).
-- The four CORE auth/user tables shipped with NO Flyway migration — only ddl-auto=
-- create-drop (the H2 reference workload) materialised them, so a fork-receiver running
-- real Flyway in production would start with NO users / refresh_tokens / verification_tokens
-- / provider_links schema at all. This file documents the canonical DDL, matching the
-- UserEntity / RefreshToken / VerificationToken / ProviderLink @Entity definitions, and
-- retires those four entries from practices/evals/.entity-migration-allowlist.txt.
--
-- ddl-auto=create-drop still manages the in-repo H2 schema (there is NO Flyway dependency
-- on the classpath — see build.gradle.kts; these V###*.sql files are documentation that a
-- prod fork-receiver applies via their own Flyway/Liquibase). Once applied, NEVER edit this
-- file (Flyway checksum). Dependency order: users first, then the three user_id FKs.
-- Postgres + H2 compatible (UUID / TIMESTAMP / BOOLEAN / VARCHAR).

CREATE TABLE users (
    id               UUID         PRIMARY KEY,
    email            VARCHAR(255) NOT NULL UNIQUE,
    hashed_password  VARCHAR(255),
    role             VARCHAR(255) NOT NULL,
    email_verified   BOOLEAN      NOT NULL,
    created_at       TIMESTAMP    NOT NULL
);

CREATE TABLE provider_links (
    id                UUID         PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users(id),
    provider          VARCHAR(255) NOT NULL,
    provider_user_id  VARCHAR(255) NOT NULL,
    provider_email    VARCHAR(255),
    linked_at         TIMESTAMP    NOT NULL,
    CONSTRAINT uq_provider_links_provider_user UNIQUE (provider, provider_user_id)
);

CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     UUID         NOT NULL REFERENCES users(id),
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL,
    revoked_at  TIMESTAMP
);

CREATE TABLE verification_tokens (
    id          UUID         PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     UUID         NOT NULL REFERENCES users(id),
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL,
    token_type  VARCHAR(255) NOT NULL
);

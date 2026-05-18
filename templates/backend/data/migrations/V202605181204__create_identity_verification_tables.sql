-- V202605181204__create_identity_verification_tables.sql
--
-- Creates tables for the identity-verification domain (SP31):
--   verified_identity
--
-- All tables extend BaseEntity columns:
--   id, created_at, updated_at, version, deleted_at, created_by, last_modified_by
-- Soft-delete is handled by @SQLDelete (UPDATE SET deleted_at) + @Where(deleted_at IS NULL).
--
-- Once applied this file MUST NOT be edited — Flyway checksum validation will reject
-- any modification. New changes go in a new V{N+1}__... file.
--
-- Rules: no-rrn-collection-without-legal-basis.md (IDV-CALLBACK-003)
--        soft-delete-only-on-base-entity.md (PRACTICES-PERS-005)
--
-- CRITICAL: NO rrn / resident_registration_number column.
-- CI/DI tokens replace the RRN for identity correlation (개인정보보호법 §24).

-- ─── verified_identity ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS verified_identity (
    -- BaseEntity-inherited columns
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP   NOT NULL,
    version             BIGINT      NOT NULL DEFAULT 0,
    deleted_at          TIMESTAMP   NULL,
    created_by          VARCHAR(255) NULL,
    last_modified_by    VARCHAR(255) NULL,

    -- VerifiedIdentity-specific columns
    ci                  VARCHAR(128)    NOT NULL,   -- Connecting Information (64 hex chars)
    di                  VARCHAR(128)    NOT NULL,   -- Duplicate Information (64 hex chars)
    name                VARCHAR(100)    NOT NULL,   -- verified legal name
    dob                 DATE            NULL,       -- date of birth from verified identity
    verified_at         TIMESTAMP       NOT NULL,   -- server-side persistence timestamp
    provider_name       VARCHAR(20)     NOT NULL,   -- "pass" | "kcb"
    metadata            JSONB           NULL        -- provider-specific extras (NOT the RRN)
);

-- Matches @Index on VerifiedIdentity entity
CREATE INDEX IF NOT EXISTS idx_verified_identity_ci
    ON verified_identity (ci) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_verified_identity_provider
    ON verified_identity (provider_name) WHERE deleted_at IS NULL;

-- Composite: provider + verified_at for time-bounded lookups (referenced in review)
CREATE INDEX IF NOT EXISTS idx_verified_identity_provider_verified_at
    ON verified_identity (provider_name, verified_at) WHERE deleted_at IS NULL;

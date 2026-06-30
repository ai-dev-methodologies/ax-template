-- ISSUE-LIFECYCLE: add issuance_status to security_token_registers.
-- Existing rows default to DRAFT (pre-issuance state).
-- CHECK mirrors V077 pattern — only the two valid enum values are accepted at the DB layer.
ALTER TABLE security_token_registers
    ADD COLUMN issuance_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_issuance_status CHECK (issuance_status IN ('DRAFT', 'ISSUED'));

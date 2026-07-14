-- V091__create_routing_rule.sql
-- WF-ROUTE-001/002 — attribute-resolved approval routing (P3-15, extends approval-workflow).
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration a Flyway-enabled production deployment would execute.

CREATE TABLE IF NOT EXISTS routing_rules (
    id                          UUID          PRIMARY KEY,
    category_or_dept            VARCHAR(64)   NOT NULL,
    min_amount                  DECIMAL(15,2) NOT NULL,
    max_amount                  DECIMAL(15,2),
    approver_role_chain_json    VARCHAR(2000) NOT NULL,
    created_at                  TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_routing_rules_category_min
    ON routing_rules(category_or_dept, min_amount);

-- WF-ROUTE-001 routing-mode attributes on the existing approval_requests table
-- (see V018__create_approval_workflow.sql). category/amount are captured once at
-- creation; resolved_chain_json is written once, later, at submission.
ALTER TABLE approval_requests ADD COLUMN IF NOT EXISTS category VARCHAR(64);
ALTER TABLE approval_requests ADD COLUMN IF NOT EXISTS amount DECIMAL(15,2);
ALTER TABLE approval_requests ADD COLUMN IF NOT EXISTS resolved_chain_json VARCHAR(4096);

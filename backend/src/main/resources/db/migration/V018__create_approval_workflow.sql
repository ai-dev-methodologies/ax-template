-- V018__create_approval_workflow.sql
-- Approval-workflow domain schema (R31).
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration that a Flyway-enabled production deployment would execute.
--
-- Trace:
--   WF-AUTHZ-002        — every request lookup filters on requester_user_id
--   WF-LIFECYCLE-001..4 — request.status mutated only via ApprovalRequestStateMachine
--   WF-STEP-001/2/3     — step.actedBy + acted_at + comment captured atomically with status
--   WF-QUERY-001        — inbox index = (approver_user_id, status) for fast scan

CREATE TABLE IF NOT EXISTS approval_requests (
    id                 UUID         PRIMARY KEY,
    requester_user_id  VARCHAR(255) NOT NULL,
    type               VARCHAR(64)  NOT NULL,
    title              VARCHAR(128),
    payload_json       VARCHAR(16384),
    status             VARCHAR(16)  NOT NULL,
    created_at         TIMESTAMP    NOT NULL,
    submitted_at       TIMESTAMP,
    completed_at       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_approval_requests_requester_created
    ON approval_requests(requester_user_id, created_at);

CREATE INDEX IF NOT EXISTS ix_approval_requests_status
    ON approval_requests(status);

CREATE TABLE IF NOT EXISTS approval_steps (
    id                 UUID         PRIMARY KEY,
    request_id         UUID         NOT NULL,
    order_index        INTEGER      NOT NULL,
    approver_user_id   VARCHAR(255) NOT NULL,
    status             VARCHAR(16)  NOT NULL,
    acted_by_user_id   VARCHAR(255),
    acted_at           TIMESTAMP,
    comment            VARCHAR(1024),
    CONSTRAINT fk_approval_steps_request
        FOREIGN KEY (request_id) REFERENCES approval_requests(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_approval_steps_request_order
    ON approval_steps(request_id, order_index);

CREATE INDEX IF NOT EXISTS ix_approval_steps_approver_status
    ON approval_steps(approver_user_id, status);

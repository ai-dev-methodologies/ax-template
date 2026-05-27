-- R60 iter1 F10 closure via Wave D2 — email template versioning + immutable history.
-- Forensic audit: "which template version produced this enqueue" joins
-- email_outbox.created_at against email_template_history.captured_at.

ALTER TABLE email_templates
    ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

CREATE TABLE email_template_history (
    id                UUID         PRIMARY KEY,
    template_code     VARCHAR(64)  NOT NULL,
    version           INTEGER      NOT NULL,
    subject_template  VARCHAR(998) NOT NULL,
    body_template     TEXT         NOT NULL,
    captured_at       TIMESTAMP    NOT NULL,
    CONSTRAINT uq_email_template_history_code_version UNIQUE (template_code, version)
);

CREATE INDEX ix_email_template_history_code_captured
    ON email_template_history (template_code, captured_at);

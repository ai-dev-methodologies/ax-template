-- R51 — email-outbox L4 domain. EMAIL-QUEUE-001 / EMAIL-SEND-001/002 / EMAIL-RETRY-001/002 / EMAIL-TEMPLATE-001 / EMAIL-ADMIN-001.

CREATE TABLE email_templates (
    template_code     VARCHAR(64) PRIMARY KEY,
    subject_template  VARCHAR(998) NOT NULL,
    body_template     TEXT NOT NULL
);

CREATE TABLE email_outbox (
    id              UUID PRIMARY KEY,
    recipient       VARCHAR(320) NOT NULL,
    template_code   VARCHAR(64)  NOT NULL,
    subject         VARCHAR(998) NOT NULL,
    body            TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP    NULL,
    last_error      VARCHAR(1024) NULL,
    created_at      TIMESTAMP    NOT NULL,
    sent_at         TIMESTAMP    NULL,
    CONSTRAINT chk_email_outbox_status CHECK (status IN ('PENDING','RETRY','SENT','DLQ'))
);

CREATE INDEX ix_email_outbox_status        ON email_outbox (status);
CREATE INDEX ix_email_outbox_next_attempt  ON email_outbox (next_attempt_at);

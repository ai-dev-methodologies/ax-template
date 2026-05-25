package com.ax.template.authblueprint.emailoutbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox row persisting one queued email send attempt-chain.
 *
 * <p>Trace:
 * <ul>
 *   <li>EMAIL-QUEUE-001 — created by {@link EmailOutboxService#enqueue}
 *       with status {@link EmailOutboxStatus#PENDING} and zero retryCount.</li>
 *   <li>EMAIL-SEND-002 — on send failure {@code retryCount} is incremented
 *       and {@code nextAttemptAt} set via exponential backoff
 *       (2^retryCount × 30 s).</li>
 *   <li>EMAIL-RETRY-001 — after 3 consecutive failures the row moves to
 *       {@link EmailOutboxStatus#DLQ}.</li>
 * </ul>
 */
@Entity
@Table(
    name = "email_outbox",
    indexes = {
        @Index(name = "ix_email_outbox_status", columnList = "status"),
        @Index(name = "ix_email_outbox_next_attempt", columnList = "next_attempt_at")
    }
)
public class EmailOutbox {

    /** EMAIL-RETRY-001 — terminal-failure threshold. */
    public static final int MAX_RETRIES = 3;

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "recipient", nullable = false, length = 320, updatable = false)
    private String recipient;

    @Column(name = "template_code", nullable = false, length = 64, updatable = false)
    private String templateCode;

    @Column(name = "subject", nullable = false, length = 998, updatable = false)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private EmailOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 1024)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected EmailOutbox() {}

    private EmailOutbox(UUID id, String recipient, String templateCode,
                        String subject, String body, Instant createdAt) {
        this.id = id;
        this.recipient = recipient;
        this.templateCode = templateCode;
        this.subject = subject;
        this.body = body;
        this.status = EmailOutboxStatus.PENDING;
        this.retryCount = 0;
        this.nextAttemptAt = null;
        this.lastError = null;
        this.createdAt = createdAt;
        this.sentAt = null;
    }

    /** EMAIL-QUEUE-001 — factory for a newly enqueued PENDING row. */
    public static EmailOutbox create(String recipient, String templateCode,
                                     String subject, String body, Instant createdAt) {
        return new EmailOutbox(UUID.randomUUID(), recipient, templateCode, subject, body, createdAt);
    }

    /** EMAIL-SEND-001 — transition PENDING/RETRY → SENT on successful delivery. */
    void markSent(Instant when) {
        this.status = EmailOutboxStatus.SENT;
        this.sentAt = when;
        this.lastError = null;
        this.nextAttemptAt = null;
    }

    /**
     * EMAIL-SEND-002 + EMAIL-RETRY-001 — record a failure: increment retryCount,
     * compute next backoff window, and transition to DLQ once MAX_RETRIES is hit.
     */
    void markFailure(String reason, Instant now, java.util.function.LongFunction<Instant> backoff) {
        this.retryCount = this.retryCount + 1;
        this.lastError = reason;
        if (this.retryCount >= MAX_RETRIES) {
            this.status = EmailOutboxStatus.DLQ;
            this.nextAttemptAt = null;
        } else {
            this.status = EmailOutboxStatus.RETRY;
            // 2^retryCount × 30s
            long delaySeconds = (1L << this.retryCount) * 30L;
            this.nextAttemptAt = backoff.apply(delaySeconds);
            // simpler default if caller didn't supply: now + delaySeconds
            if (this.nextAttemptAt == null) {
                this.nextAttemptAt = now.plusSeconds(delaySeconds);
            }
        }
    }

    /** Admin-triggered retry from DLQ → PENDING (operator decision; resets retryCount). */
    void resetForRetry() {
        this.status = EmailOutboxStatus.PENDING;
        this.retryCount = 0;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    public UUID getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getTemplateCode() { return templateCode; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public EmailOutboxStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
}

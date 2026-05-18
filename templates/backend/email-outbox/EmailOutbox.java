/**
 * @ax-template-meta
 * template_id: backend/email-outbox/EmailOutbox
 * layer: backend-domain
 * domain: email-outbox
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Entity mapping with @Entity, @Id, @GeneratedValue"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html"
 *   - source_type: external
 *     citation: "Transactional Outbox Pattern — microservices.io"
 *     url: "https://microservices.io/patterns/data/transactional-outbox.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   EmailOutbox entity stores one pending outbound email.
 *   Status lifecycle: PENDING → SENT | PENDING → RETRY (up to MAX_RETRIES) → DLQ.
 *   Extends BaseEntity (from SP13) for: id (UUID), createdAt, updatedAt, deleted.
 */
package com.example.app.emailoutbox;

import com.example.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLDelete;

/**
 * Email outbox entry — represents one outbound email queued for delivery.
 *
 * <p>Lifecycle:
 * <pre>
 *   PENDING (enqueued) → SENT (delivered)
 *                      → RETRY (failed, retryCount < MAX_RETRIES, nextAttemptAt set)
 *                      → DLQ (failed, retryCount >= MAX_RETRIES)
 * </pre>
 *
 * <p>processQueue() picks up PENDING and RETRY entries where nextAttemptAt <= now,
 * attempts delivery via EmailSenderService, and updates the status accordingly.
 *
 * <p>Extends {@code BaseEntity} (SP13) for: id, createdAt, updatedAt, deleted.
 */
@Entity
@SQLDelete(sql = "UPDATE email_outbox SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(
    name = "email_outbox",
    indexes = {
        @Index(name = "idx_email_outbox_status_next", columnList = "status, next_attempt_at"),
        @Index(name = "idx_email_outbox_created", columnList = "created_at"),
    }
)
public class EmailOutbox extends BaseEntity {

    // ─── recipient ─────────────────────────────────────────────────────────

    @Column(name = "recipient", nullable = false, length = 320)
    private String recipient;

    // ─── content ───────────────────────────────────────────────────────────

    @Column(name = "subject", nullable = false, length = 998)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    // ─── lifecycle ─────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private EmailOutboxStatus status = EmailOutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    /** Next scheduled send attempt; null for PENDING (attempt immediately). */
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    /** Timestamp when the email was successfully sent; null until SENT. */
    @Column(name = "sent_at")
    private Instant sentAt;

    /** Reason stored when entry moves to DLQ; holds last exception message. */
    @Column(name = "dlq_reason", columnDefinition = "TEXT")
    private String dlqReason;

    // ─── constructors ──────────────────────────────────────────────────────

    protected EmailOutbox() {
        // JPA
    }

    /**
     * Factory — creates a new PENDING outbox entry.
     *
     * @param recipient email address (max 320 chars per RFC 5321)
     * @param subject   email subject line (max 998 chars per RFC 5322)
     * @param body      rendered HTML or plain text body
     */
    public static EmailOutbox create(String recipient, String subject, String body) {
        var entry = new EmailOutbox();
        entry.recipient = recipient;
        entry.subject = subject;
        entry.body = body;
        entry.status = EmailOutboxStatus.PENDING;
        entry.retryCount = 0;
        return entry;
    }

    // ─── domain transitions ────────────────────────────────────────────────

    /**
     * Marks this entry as successfully sent.
     */
    public void markSent() {
        this.status = EmailOutboxStatus.SENT;
        this.sentAt = Instant.now();
    }

    /**
     * Marks this entry for retry with exponential backoff.
     *
     * @param newRetryCount  updated retry count (caller increments before calling)
     * @param nextAttemptAt  next scheduled attempt time
     */
    public void markRetry(int newRetryCount, Instant nextAttemptAt) {
        this.status = EmailOutboxStatus.RETRY;
        this.retryCount = newRetryCount;
        this.nextAttemptAt = nextAttemptAt;
    }

    /**
     * Moves this entry to DLQ (dead-letter queue) after all retries are exhausted.
     *
     * @param reason last exception message; stored for admin inspection
     */
    public void markDlq(String reason) {
        this.status = EmailOutboxStatus.DLQ;
        this.dlqReason = reason;
    }

    /**
     * Resets a DLQ entry to PENDING for manual admin retry.
     * Clears retryCount and dlqReason.
     */
    public void resetToPending() {
        this.status = EmailOutboxStatus.PENDING;
        this.retryCount = 0;
        this.nextAttemptAt = null;
        this.dlqReason = null;
    }

    // ─── getters ───────────────────────────────────────────────────────────

    public String getRecipient()            { return recipient; }
    public String getSubject()              { return subject; }
    public String getBody()                 { return body; }
    public EmailOutboxStatus getStatus()    { return status; }
    public int getRetryCount()              { return retryCount; }
    public Instant getNextAttemptAt()       { return nextAttemptAt; }
    public Instant getSentAt()              { return sentAt; }
    public String getDlqReason()            { return dlqReason; }

    // ─── enum ──────────────────────────────────────────────────────────────

    public enum EmailOutboxStatus {
        PENDING, SENT, RETRY, DLQ
    }
}

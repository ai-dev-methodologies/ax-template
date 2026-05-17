/**
 * @ax-template-meta
 * template_id: backend/email-outbox/EmailOutboxService
 * layer: backend-domain
 * domain: email-outbox
 * anchors_rule: api-controller-service-separation.md (PRACTICES-API-003)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Transactional Outbox Pattern — microservices.io"
 *     url: "https://microservices.io/patterns/data/transactional-outbox.html"
 *   - source_type: external
 *     citation: "OWASP ASVS V4 — Verify that the application limits failed authentication attempts"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 *   - source_type: external
 *     citation: "Spring Framework Reference — @Scheduled annotation for polling loops"
 *     url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   EmailOutboxService owns all business logic for the email-outbox domain:
 *     - enqueue(): persist a PENDING entry (does NOT send)
 *     - processQueue(): scheduled loop — picks up PENDING/RETRY, calls EmailSenderService
 *     - previewTemplate(): render a template string without persisting or sending
 *   MAX_RETRIES = 3; exponential backoff = 2^retryCount × 30 seconds.
 */
package com.example.app.emailoutbox;

import com.example.app.common.BaseService;
import com.example.app.emailoutbox.EmailOutbox.EmailOutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for the email-outbox domain.
 *
 * <p>Pattern: Transactional Outbox — email is persisted to the database before any
 * SMTP call; the processQueue() loop handles delivery asynchronously with retry/DLQ.
 *
 * <p>Retry policy (EMAIL-RETRY-001, EMAIL-RETRY-002):
 * <ul>
 *   <li>MAX_RETRIES = 3 consecutive failures → DLQ
 *   <li>Backoff: {@code backoffSeconds = 2^retryCount × 30}
 *   <li>RETRY entries with future nextAttemptAt are skipped in the current cycle
 * </ul>
 *
 * <p>Extends {@link BaseService} (SP13) for shared exception helpers.
 */
@Service
@Transactional(readOnly = true)
public class EmailOutboxService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxService.class);

    /** Maximum send failures before an entry is moved to DLQ. */
    static final int MAX_RETRIES = 3;

    private final EmailOutboxRepository outboxRepository;
    private final EmailSenderService senderService;
    private final EmailTemplateService templateService;

    public EmailOutboxService(
            EmailOutboxRepository outboxRepository,
            EmailSenderService senderService,
            EmailTemplateService templateService) {
        this.outboxRepository = outboxRepository;
        this.senderService = senderService;
        this.templateService = templateService;
    }

    // ─── enqueue ──────────────────────────────────────────────────────────

    /**
     * Queues an email for delivery using a named template.
     *
     * <p>Does NOT invoke EmailSenderService — no SMTP call occurs here.
     * The entry is persisted as PENDING and picked up by the next processQueue() cycle.
     *
     * @param recipient    target email address
     * @param templateCode template identifier (e.g., "forgot-password")
     * @param templateVars variable map substituted into the template
     * @return persisted EmailOutbox entry with status PENDING
     */
    @Transactional
    public EmailOutbox enqueue(String recipient, String templateCode, Object templateVars) {
        var rendered = templateService.render(templateCode, templateVars);
        var entry = EmailOutbox.create(recipient, rendered.subject(), rendered.body());
        return outboxRepository.save(entry);
    }

    // ─── processQueue (scheduled) ─────────────────────────────────────────

    /**
     * Scheduled send loop — runs every 60 seconds by default.
     *
     * <p>Picks up PENDING and RETRY-eligible entries (nextAttemptAt &lt;= now)
     * and attempts delivery via EmailSenderService. On success: status → SENT.
     * On failure: applies exponential backoff (EMAIL-SEND-002) or DLQ (EMAIL-RETRY-001).
     */
    @Scheduled(fixedDelayString = "${ax.email.outbox.poll-interval-ms:60000}")
    @Transactional
    public void processQueue() {
        List<EmailOutbox> eligible = outboxRepository.findAllPendingAndRetry(Instant.now());
        if (!eligible.isEmpty()) {
            log.debug("EmailOutbox processQueue: {} entries to process", eligible.size());
        }
        eligible.forEach(this::trySend);
    }

    // ─── preview ──────────────────────────────────────────────────────────

    /**
     * Renders a template to its subject + body string without persisting or sending.
     *
     * <p>Safe to call in admin preview UI — no side effects (EMAIL-QUEUE-002).
     *
     * @param templateCode template identifier
     * @param templateVars variable map substituted into the template
     * @return rendered email body string
     */
    public EmailTemplateService.RenderedEmail previewTemplate(
            String templateCode, Object templateVars) {
        return templateService.render(templateCode, templateVars);
    }

    // ─── admin operations ─────────────────────────────────────────────────

    /**
     * Returns paginated outbox entries for the admin view.
     *
     * @param statusFilter null = ALL; otherwise filter by specific status
     */
    public Page<EmailOutbox> listForAdmin(EmailOutboxStatus statusFilter, Pageable pageable) {
        return outboxRepository.findAllForAdmin(statusFilter, pageable);
    }

    /**
     * Returns a single outbox entry for the admin view.
     *
     * @throws jakarta.persistence.EntityNotFoundException if not found
     */
    public EmailOutbox getForAdmin(UUID id) {
        return outboxRepository.findActiveById(id)
                .orElseThrow(() -> entityNotFound("EmailOutbox", id));
    }

    /**
     * Cancels a PENDING or RETRY entry by moving it to DLQ with reason "admin-cancelled".
     *
     * @throws IllegalStateException if entry is already SENT
     */
    @Transactional
    public void cancel(UUID id) {
        var entry = getForAdmin(id);
        if (entry.getStatus() == EmailOutboxStatus.SENT) {
            throw new IllegalStateException("Cannot cancel a SENT email outbox entry");
        }
        entry.markDlq("admin-cancelled");
        outboxRepository.save(entry);
    }

    /**
     * Resets a DLQ entry to PENDING for the next processQueue() cycle.
     *
     * @throws IllegalStateException if entry is not in DLQ status
     */
    @Transactional
    public EmailOutbox retryFromDlq(UUID id) {
        var entry = getForAdmin(id);
        if (entry.getStatus() != EmailOutboxStatus.DLQ) {
            throw new IllegalStateException("Entry is not in DLQ status — cannot retry");
        }
        entry.resetToPending();
        return outboxRepository.save(entry);
    }

    // ─── internal helpers ─────────────────────────────────────────────────

    /**
     * Attempts to send one outbox entry.
     *
     * <p>On success: marks SENT. On failure: increments retryCount.
     * If retryCount reaches MAX_RETRIES, transitions to DLQ.
     * Otherwise, sets nextAttemptAt using exponential backoff.
     */
    private void trySend(EmailOutbox entry) {
        try {
            senderService.send(entry.getRecipient(), entry.getSubject(), entry.getBody());
            entry.markSent();
            log.info("EmailOutbox SENT id={}", entry.getId());
        } catch (Exception ex) {
            int newRetryCount = entry.getRetryCount() + 1;
            if (newRetryCount >= MAX_RETRIES) {
                entry.markDlq(ex.getMessage());
                log.error("EmailOutbox DLQ id={} reason={}", entry.getId(), ex.getMessage());
            } else {
                long backoffSeconds = (long) Math.pow(2, newRetryCount) * 30L;
                entry.markRetry(newRetryCount, Instant.now().plusSeconds(backoffSeconds));
                log.warn("EmailOutbox RETRY id={} attempt={} nextIn={}s",
                        entry.getId(), newRetryCount, backoffSeconds);
            }
        }
        outboxRepository.save(entry);
    }
}

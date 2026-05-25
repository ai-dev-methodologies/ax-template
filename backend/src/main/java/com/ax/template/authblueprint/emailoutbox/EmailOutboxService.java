package com.ax.template.authblueprint.emailoutbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailOutboxService {

    // R52 lesson — structured audit emission distinct from per-row read
    // (BULK and individual stay separable downstream).
    private static final Logger AUDIT = LoggerFactory.getLogger("audit.email-outbox");

    private final EmailOutboxRepository outboxRepository;
    private final EmailTemplateService templateService;
    private final EmailSenderService senderService;
    private final Clock clock;

    public EmailOutboxService(EmailOutboxRepository outboxRepository,
                              EmailTemplateService templateService,
                              EmailSenderService senderService,
                              Clock clock) {
        this.outboxRepository = outboxRepository;
        this.templateService = templateService;
        this.senderService = senderService;
        this.clock = clock;
    }

    /** EMAIL-QUEUE-001 — render via template, persist as PENDING. No send. */
    @Transactional
    public EmailOutbox enqueue(String recipient, String templateCode, Map<String, String> vars) {
        EmailTemplateService.Rendered rendered = templateService.render(templateCode, vars);
        Instant now = Instant.now(clock);
        EmailOutbox row = EmailOutbox.create(recipient, templateCode, rendered.subject(),
                                             rendered.body(), now);
        return outboxRepository.save(row);
    }

    /** EMAIL-QUEUE-002 — render without persisting, without sending. */
    @Transactional(readOnly = true)
    public EmailTemplateService.Rendered previewTemplate(String templateCode,
                                                          Map<String, String> vars) {
        return templateService.render(templateCode, vars);
    }

    /**
     * EMAIL-SEND-001 + EMAIL-SEND-002 + EMAIL-RETRY-001 + EMAIL-RETRY-002 —
     * iterate the due rows (PENDING or RETRY whose nextAttemptAt is past),
     * attempt send each, update outbox row to SENT on success or
     * RETRY/DLQ on failure.
     */
    @Transactional
    public int processQueue() {
        Instant now = Instant.now(clock);
        List<EmailOutbox> due = outboxRepository.findDueForSending(now);
        int processed = 0;
        for (EmailOutbox row : due) {
            try {
                senderService.send(row.getRecipient(), row.getSubject(), row.getBody());
                row.markSent(now);
            } catch (EmailSendException ex) {
                // Bounded-length reason; the catalog client applies the
                // R50 stored-server-error-sanitize-at-render-layer rule
                // before rendering this on the admin view.
                String reason = ex.getMessage() == null
                    ? "unknown error"
                    : (ex.getMessage().length() > 1000
                        ? ex.getMessage().substring(0, 1000)
                        : ex.getMessage());
                row.markFailure(reason, now, delay -> now.plusSeconds(delay));
            }
            processed++;
        }
        return processed;
    }

    /** EMAIL-ADMIN-001 — admin list with optional status filter. */
    @Transactional(readOnly = true)
    public Page<EmailOutbox> adminList(EmailOutboxStatus statusFilter, int page, int size) {
        int clampedSize = Math.min(Math.max(size, 1), 100);
        int clampedPage = Math.max(page, 0);
        PageRequest pageReq = PageRequest.of(clampedPage, clampedSize);
        if (statusFilter == null) {
            return outboxRepository.findAllByOrderByCreatedAtDesc(pageReq);
        }
        return outboxRepository.findByStatus(statusFilter, pageReq);
    }

    /** Admin retry: DLQ or RETRY row → reset to PENDING for next processQueue cycle. */
    @Transactional
    public EmailOutbox adminRetry(UUID id) {
        EmailOutbox row = outboxRepository.findById(id)
            .orElseThrow(() -> new EmailOutboxNotFoundException(id));
        if (row.getStatus() == EmailOutboxStatus.SENT) {
            // Cannot replay an already-sent email — would duplicate side effects.
            throw new IllegalStateException("cannot retry SENT email: " + id);
        }
        row.resetForRetry();
        AUDIT.info("verb=ADMIN_RETRY id={} recipient={}", id, row.getRecipient());
        return outboxRepository.save(row);
    }

    /** Admin delete: remove an outbox row entirely (operator decision). */
    @Transactional
    public void adminDelete(UUID id) {
        EmailOutbox row = outboxRepository.findById(id).orElse(null);
        if (row == null) return; // RFC 9110 §9.3.5 idempotent — 204 on absent target
        outboxRepository.delete(row);
        AUDIT.info("verb=ADMIN_DELETE id={} recipient={}", id, row.getRecipient());
    }

    @Transactional(readOnly = true)
    public EmailOutbox adminGet(UUID id) {
        return outboxRepository.findById(id)
            .orElseThrow(() -> new EmailOutboxNotFoundException(id));
    }
}

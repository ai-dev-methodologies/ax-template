/**
 * FIXTURE: email_outbox/pass
 * Demonstrates correct email-outbox service:
 * - enqueue() creates PENDING record
 * - processQueue() sends and marks SENT
 * - send failure increments retryCount and moves to RETRY then DLQ
 * - previewTemplate() renders without sending
 */
package com.example.fixture.email_outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmailOutboxService {

    private final EmailOutboxRepository outboxRepository;
    private final EmailSenderService senderService;
    private final EmailTemplateService templateService;

    private static final int MAX_RETRIES = 3;

    public EmailOutboxService(
            EmailOutboxRepository outboxRepository,
            EmailSenderService senderService,
            EmailTemplateService templateService) {
        this.outboxRepository = outboxRepository;
        this.senderService = senderService;
        this.templateService = templateService;
    }

    @Transactional
    public EmailOutbox enqueue(String recipient, String templateCode, Object templateVars) {
        var rendered = templateService.render(templateCode, templateVars);
        var entry = EmailOutbox.create(recipient, rendered.subject(), rendered.body());
        return outboxRepository.save(entry);
    }

    @Transactional
    public void processQueue() {
        List<EmailOutbox> pending = outboxRepository.findAllPendingAndRetry(Instant.now());
        for (EmailOutbox entry : pending) {
            trySend(entry);
        }
    }

    private void trySend(EmailOutbox entry) {
        try {
            senderService.send(entry.getRecipient(), entry.getSubject(), entry.getBody());
            entry.markSent();
            outboxRepository.save(entry);
        } catch (Exception ex) {
            int retries = entry.getRetryCount() + 1;
            if (retries >= MAX_RETRIES) {
                entry.markDlq(ex.getMessage());
            } else {
                long backoffSeconds = (long) Math.pow(2, retries) * 30L;
                entry.markRetry(retries, Instant.now().plusSeconds(backoffSeconds));
            }
            outboxRepository.save(entry);
        }
    }

    public String previewTemplate(String templateCode, Object templateVars) {
        return templateService.render(templateCode, templateVars).body();
    }
}

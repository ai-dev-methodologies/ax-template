/**
 * FIXTURE: email_outbox/fail_no_retry_on_failure
 * Demonstrates WRONG pattern: send failure swallows error, no retry/DLQ transition.
 * This violates email-outbox retry policy (EMAIL-RETRY-001).
 */
package com.example.fixture.email_outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmailOutboxService {

    private final EmailOutboxRepository outboxRepository;
    private final EmailSenderService senderService;

    public EmailOutboxService(EmailOutboxRepository outboxRepository, EmailSenderService senderService) {
        this.outboxRepository = outboxRepository;
        this.senderService = senderService;
    }

    @Transactional
    public void processQueue() {
        List<EmailOutbox> pending = outboxRepository.findAllPending();
        for (EmailOutbox entry : pending) {
            try {
                senderService.send(entry.getRecipient(), entry.getSubject(), entry.getBody());
                entry.markSent();
            } catch (Exception ex) {
                // BUG: silently swallowed — no retry, no DLQ, no status update
            }
            outboxRepository.save(entry);
        }
    }
}

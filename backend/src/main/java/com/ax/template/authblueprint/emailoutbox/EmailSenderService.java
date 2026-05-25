package com.ax.template.authblueprint.emailoutbox;

/**
 * Side-effecting SMTP / SES adapter — sole job is to attempt one outbound
 * delivery. The outbox transition logic (PENDING/RETRY → SENT/DLQ) lives
 * in {@link EmailOutboxService}; this interface is intentionally narrow
 * so fork-receivers can swap the underlying provider (SMTP, AWS SES,
 * SendGrid, Mailgun) without touching the catalog logic.
 *
 * <p>Implementations MUST throw on transient failures (network, 5xx) so
 * {@link EmailOutboxService#processQueue} can flip the row to RETRY +
 * exponential backoff (EMAIL-SEND-002). Permanent failures (4xx invalid
 * recipient) should also throw — the catalog policy is "always retry up
 * to MAX_RETRIES" so the operator can decide via the admin surface
 * whether to give up or fix the recipient and replay.
 */
public interface EmailSenderService {
    void send(String recipient, String subject, String body) throws EmailSendException;
}

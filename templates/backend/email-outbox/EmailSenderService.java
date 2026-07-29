/**
 * @ax-template-meta
 * template_id: backend/email-outbox/EmailSenderService
 * layer: backend-domain
 * domain: email-outbox
 * anchors_rule_absent: outbound-email PORT interface. The nearest catalog abstraction rule
 *   (messaging-publisher-interface) is scoped to broker SDKs leaking into the domain via a
 *   MessagePublisher; anchoring an SMTP/provider port there would over-claim. Enumerated in
 *   JAVA_NO_ANCHOR_EXEMPT in practices/evals/evidence_guard.sh.
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework — JavaMailSender interface for SMTP email"
 *     url: "https://docs.spring.io/spring-framework/reference/integration/email.html"
 *   - source_type: external
 *     citation: "OWASP Email Security Cheat Sheet — header injection, recipient validation"
 *     url: "https://cheatsheetseries.owasp.org/cheatsheets/Email_Security_Cheat_Sheet.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   EmailSenderService is the interface for all outbound email delivery.
 *   Two implementations are provided:
 *     - SmtpEmailGateway: production SMTP via Spring JavaMailSender (or SES)
 *     - MockEmailGateway: test double that captures messages in-memory
 *   EmailOutboxService always calls this interface; swap implementations via Spring bean config.
 */
package com.example.app.emailoutbox;

/**
 * Outbound email delivery abstraction.
 *
 * <p>EmailOutboxService calls this interface during processQueue(); the
 * concrete implementation is resolved by Spring's bean registry. Use
 * {@code SmtpEmailGateway} in production and {@code MockEmailGateway} in tests.
 *
 * <p>Implementations must:
 * <ul>
 *   <li>Throw a runtime exception on delivery failure so the caller can apply retry/DLQ logic.
 *   <li>Not catch or swallow exceptions — the outbox service owns the retry decision.
 *   <li>Sanitize recipient and subject against header-injection attacks before sending.
 * </ul>
 */
public interface EmailSenderService {

    /**
     * Sends a plain-text or HTML email.
     *
     * @param recipient target email address (RFC 5321, max 320 chars)
     * @param subject   email subject (RFC 5322, max 998 chars)
     * @param body      rendered email body (HTML or plain text)
     * @throws EmailSendException on any SMTP or transport failure
     */
    void send(String recipient, String subject, String body);

    // ─── SmtpEmailGateway (inner class template) ───────────────────────────

    /**
     * Production gateway using Spring's {@code JavaMailSender}.
     *
     * <p>Configure via {@code spring.mail.*} properties.
     * Swap with AWS SES, SendGrid, or other provider by replacing this bean.
     *
     * <p>Usage: declare as @Bean or @Component; auto-detected by Spring.
     */
    class SmtpEmailGateway implements EmailSenderService {

        private final org.springframework.mail.javamail.JavaMailSender mailSender;
        private final String fromAddress;

        public SmtpEmailGateway(
                org.springframework.mail.javamail.JavaMailSender mailSender,
                String fromAddress) {
            this.mailSender = mailSender;
            this.fromAddress = fromAddress;
        }

        @Override
        public void send(String recipient, String subject, String body) {
            try {
                var message = mailSender.createMimeMessage();
                var helper = new org.springframework.mail.javamail.MimeMessageHelper(
                        message, false, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(recipient);
                helper.setSubject(subject);
                // body is treated as HTML; set second arg to false for plain-text
                helper.setText(body, true);
                mailSender.send(message);
            } catch (Exception ex) {
                throw new EmailSendException("SMTP delivery failed: " + ex.getMessage(), ex);
            }
        }
    }

    // ─── MockEmailGateway (inner class template) ───────────────────────────

    /**
     * Test double that captures sent messages in an in-memory list.
     *
     * <p>Use in unit and integration tests:
     * <pre>
     *   var mock = new EmailSenderService.MockEmailGateway();
     *   // inject mock into EmailOutboxService
     *   service.processQueue();
     *   assertThat(mock.getSent()).hasSize(1);
     *   assertThat(mock.getSent().get(0).recipient()).isEqualTo("user@example.com");
     * </pre>
     */
    class MockEmailGateway implements EmailSenderService {

        private final java.util.List<SentEmail> sent = new java.util.ArrayList<>();
        private boolean shouldFail = false;
        private String failureMessage = "Mock send failure";

        /** Configure mock to throw on next send call(s). */
        public void configureFail(String message) {
            this.shouldFail = true;
            this.failureMessage = message;
        }

        /** Reset fail mode. */
        public void reset() {
            this.shouldFail = false;
            this.sent.clear();
        }

        @Override
        public void send(String recipient, String subject, String body) {
            if (shouldFail) {
                throw new EmailSendException(failureMessage, null);
            }
            sent.add(new SentEmail(recipient, subject, body));
        }

        /** Returns an unmodifiable view of captured messages. */
        public java.util.List<SentEmail> getSent() {
            return java.util.Collections.unmodifiableList(sent);
        }

        /** Captured sent email record. */
        public record SentEmail(String recipient, String subject, String body) {}
    }

    // ─── exception ─────────────────────────────────────────────────────────

    /** Thrown by gateway implementations on SMTP / transport failure. */
    class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

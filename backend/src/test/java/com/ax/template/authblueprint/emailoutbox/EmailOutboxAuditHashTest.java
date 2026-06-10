package com.ax.template.authblueprint.emailoutbox;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.ax.template.authblueprint.common.MemberWriter;

/**
 * R84 — pins the hash format that R60 introduced on EmailOutboxService's
 * ADMIN_RETRY / ADMIN_DELETE audit log lines. The R61
 * audit-log-pii-hash-required rule mandates that AUDIT.info MUST NOT
 * embed a raw recipient email; R60 wired the hash; R67 lifted the
 * helper to {@code common.AuditPiiHelper}; R84 asserts the format
 * mechanically so a refactor that reverts to {@code recipient=} fails
 * loud.
 *
 * Lightweight: pure Mockito + Logback ListAppender, no Spring context.
 */
@Tag("EMAIL")
class EmailOutboxAuditHashTest {

    private static final String AUDIT_LOGGER = "audit.email-outbox";
    private static final Instant FIXED = Instant.parse("2026-05-26T10:00:00Z");

    private EmailOutboxRepository outboxRepository;
    private EmailOutboxService service;
    private ch.qos.logback.classic.Logger auditLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(EmailOutboxRepository.class);
        EmailTemplateRepository templateRepository = mock(EmailTemplateRepository.class);
        EmailSenderService senderService = mock(EmailSenderService.class);
        MemberWriter members = mock(MemberWriter.class);
        Clock clock = Clock.fixed(FIXED, ZoneOffset.UTC);
        EmailTemplateService templateService = new EmailTemplateService(templateRepository, members, clock);
        service = new EmailOutboxService(outboxRepository, templateService, senderService, clock);

        auditLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(AUDIT_LOGGER);
        appender = new ListAppender<>();
        appender.start();
        auditLogger.addAppender(appender);
        auditLogger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        if (auditLogger != null && appender != null) {
            auditLogger.detachAppender(appender);
        }
    }

    @Test
    void adminRetry_emitsRecipientHashSixteenHexChars_neverRawEmail() {
        EmailOutbox row = EmailOutbox.create(
            "alice@example.com", "verification", "Verify your account", "body", FIXED);
        // Force into RETRY so adminRetry doesn't trip the SENT guard.
        row.markFailure("smtp error", FIXED, d -> FIXED.plusSeconds(d));
        when(outboxRepository.findById(any())).thenReturn(Optional.of(row));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.adminRetry(UUID.randomUUID());

        List<ILoggingEvent> events = appender.list.stream()
            .filter(e -> e.getFormattedMessage().contains("verb=ADMIN_RETRY"))
            .toList();
        assertThat(events).hasSize(1);

        String msg = events.get(0).getFormattedMessage();
        assertThat(msg)
            .as("audit line must contain recipientHash=<16-hex>")
            .matches(".*\\brecipientHash=[0-9a-f]{16}\\b.*");
        assertThat(msg)
            .as("audit line must NOT contain the raw email")
            .doesNotContain("alice@example.com");
        assertThat(msg)
            .as("audit line must NOT use the pre-R60 `recipient=` key name")
            .doesNotContain("recipient=alice");
    }

    @Test
    void adminDelete_emitsRecipientHashSixteenHexChars_neverRawEmail() {
        EmailOutbox row = EmailOutbox.create(
            "bob@example.com", "verification", "Verify your account", "body", FIXED);
        when(outboxRepository.findById(any())).thenReturn(Optional.of(row));

        service.adminDelete(UUID.randomUUID());

        List<ILoggingEvent> events = appender.list.stream()
            .filter(e -> e.getFormattedMessage().contains("verb=ADMIN_DELETE")
                      && !e.getFormattedMessage().contains("ADMIN_DELETE_ABSENT"))
            .toList();
        assertThat(events).hasSize(1);

        String msg = events.get(0).getFormattedMessage();
        assertThat(msg)
            .as("audit line must contain recipientHash=<16-hex>")
            .matches(".*\\brecipientHash=[0-9a-f]{16}\\b.*");
        assertThat(msg)
            .as("audit line must NOT contain the raw email")
            .doesNotContain("bob@example.com");
    }

    @Test
    void adminDelete_absentTarget_emitsAbsentVerbWithNoRecipientHash() {
        // Confirms R60 F9 closure — distinct verb for the idempotent
        // "row already gone" path. recipientHash is absent because there
        // was no row to look up.
        when(outboxRepository.findById(any())).thenReturn(Optional.empty());

        service.adminDelete(UUID.randomUUID());

        List<ILoggingEvent> events = appender.list.stream()
            .filter(e -> e.getFormattedMessage().contains("verb=ADMIN_DELETE_ABSENT"))
            .toList();
        assertThat(events).hasSize(1);

        String msg = events.get(0).getFormattedMessage();
        assertThat(msg).doesNotContain("recipientHash");
    }
}

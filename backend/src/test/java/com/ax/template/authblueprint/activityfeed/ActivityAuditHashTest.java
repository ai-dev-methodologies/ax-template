package com.ax.template.authblueprint.activityfeed;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * R84 — pins the hash format that R62 introduced on ActivityService's
 * BULK_MARK_READ audit log line. The R61 audit-log-pii-hash-required
 * rule mandates that AUDIT.info MUST NOT embed a raw user identifier;
 * R62 wired AuditPiiHelper.piiHash on that one log site; R84 mechanically
 * asserts the format so a refactor that reverts to raw {@code caller=}
 * fails the test loud.
 *
 * Lightweight: pure Mockito + Logback ListAppender, no Spring context.
 */
@Tag("ACTIVITY")
class ActivityAuditHashTest {

    private static final String AUDIT_LOGGER = "audit.activity-feed";

    private ActivityEventRepository eventRepository;
    private ActivityReadRepository readRepository;
    private ActivityService service;
    private ch.qos.logback.classic.Logger auditLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        eventRepository = mock(ActivityEventRepository.class);
        readRepository = mock(ActivityReadRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        Clock clock = Clock.fixed(Instant.parse("2026-05-26T10:00:00Z"), ZoneOffset.UTC);
        service = new ActivityService(eventRepository, readRepository, objectMapper, clock);

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
    void markAllRead_emitsCallerHashSixteenHexChars_neverRawUserId() {
        // Seed: 2 unread events for the caller.
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        when(readRepository.findUnreadEventIdsForUser(any())).thenReturn(List.of(e1, e2));
        when(readRepository.findByEventIdAndUserId(any(), any())).thenReturn(java.util.Optional.empty());

        // Caller userId is email-shaped to exercise the worst case for PII.
        String userId = "alice@example.com";

        service.markAllRead(userId);

        // Exactly one BULK_MARK_READ audit line emitted.
        List<ILoggingEvent> events = appender.list.stream()
            .filter(e -> e.getFormattedMessage().contains("verb=BULK_MARK_READ"))
            .toList();
        assertThat(events).hasSize(1);

        String msg = events.get(0).getFormattedMessage();

        // R61 invariant — callerHash is a 16-hex correlation token; raw
        // userId / email never appears in the log line.
        assertThat(msg)
            .as("audit line must contain callerHash=<16-hex>")
            .matches(".*\\bcallerHash=[0-9a-f]{16}\\b.*");
        assertThat(msg)
            .as("audit line must NOT contain the raw email")
            .doesNotContain(userId);
        assertThat(msg)
            .as("audit line must NOT use the pre-R62 `caller=` key name")
            .doesNotContain("caller=alice");
    }
}

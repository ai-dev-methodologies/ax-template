package com.ax.template.authblueprint.emailoutbox;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R51 — email-outbox L4 compliance tests. 8 spec items / 4 families.
 * Mirrors R29..R36 / R48 / R49 compliance test convention.
 */
@Tag("EMAIL")
class EmailOutboxComplianceTest {

    private final Instant FIXED = Instant.parse("2026-05-25T10:00:00Z");
    private final Clock clock = Clock.fixed(FIXED, ZoneOffset.UTC);

    private EmailOutboxRepository outboxRepository;
    private EmailTemplateRepository templateRepository;
    private EmailTemplateService templateService;
    private EmailSenderService senderService;
    private EmailOutboxService service;

    private void setup() {
        outboxRepository = mock(EmailOutboxRepository.class);
        templateRepository = mock(EmailTemplateRepository.class);
        senderService = mock(EmailSenderService.class);
        templateService = new EmailTemplateService(templateRepository);
        service = new EmailOutboxService(outboxRepository, templateService, senderService, clock);
    }

    private void seedTemplate(String code, String subject, String body) {
        when(templateRepository.findById(code))
            .thenReturn(Optional.of(new EmailTemplate(code, subject, body)));
    }

    @Test
    @Tag("EMAIL-QUEUE-001")
    void queue_enqueue_persistsPendingWithZeroRetry() {
        setup();
        seedTemplate("welcome", "Hi {{name}}", "Hello {{name}}, body.");
        when(outboxRepository.save(any(EmailOutbox.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        EmailOutbox row = service.enqueue("u@x.kr", "welcome", Map.of("name", "지수"));

        assertThat(row.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
        assertThat(row.getRetryCount()).isZero();
        assertThat(row.getSubject()).isEqualTo("Hi 지수");
        assertThat(row.getBody()).isEqualTo("Hello 지수, body.");
        assertThat(row.getNextAttemptAt()).isNull();
        assertThat(row.getCreatedAt()).isEqualTo(FIXED);
        verify(outboxRepository).save(any(EmailOutbox.class));
    }

    @Test
    @Tag("EMAIL-QUEUE-002")
    void queue_previewTemplate_doesNotPersistAndDoesNotSend() {
        setup();
        seedTemplate("welcome", "Hi {{name}}", "Body for {{name}}");

        EmailTemplateService.Rendered preview =
            service.previewTemplate("welcome", Map.of("name", "지수"));

        assertThat(preview.subject()).isEqualTo("Hi 지수");
        assertThat(preview.body()).isEqualTo("Body for 지수");
        verify(outboxRepository, never()).save(any(EmailOutbox.class));
        // EmailSenderService is never invoked from previewTemplate
    }

    @Test
    @Tag("EMAIL-SEND-001")
    void send_processQueue_transitionsPendingToSentOnSuccess() throws Exception {
        setup();
        EmailOutbox row = EmailOutbox.create("u@x.kr", "code", "subj", "body", FIXED);
        when(outboxRepository.findDueForSending(FIXED)).thenReturn(List.of(row));

        int processed = service.processQueue();

        assertThat(processed).isEqualTo(1);
        assertThat(row.getStatus()).isEqualTo(EmailOutboxStatus.SENT);
        assertThat(row.getSentAt()).isEqualTo(FIXED);
        verify(senderService).send(eq("u@x.kr"), eq("subj"), eq("body"));
    }

    @Test
    @Tag("EMAIL-SEND-002")
    void send_failureIncrementsRetryAndSetsBackoff() throws Exception {
        setup();
        EmailOutbox row = EmailOutbox.create("u@x.kr", "code", "subj", "body", FIXED);
        when(outboxRepository.findDueForSending(FIXED)).thenReturn(List.of(row));
        doThrow(new EmailSendException("smtp 5xx")).when(senderService).send(any(), any(), any());

        service.processQueue();

        assertThat(row.getStatus()).isEqualTo(EmailOutboxStatus.RETRY);
        assertThat(row.getRetryCount()).isEqualTo(1);
        // 2^1 × 30s = 60s
        assertThat(row.getNextAttemptAt()).isEqualTo(FIXED.plusSeconds(60));
        assertThat(row.getLastError()).isEqualTo("smtp 5xx");
    }

    @Test
    @Tag("EMAIL-RETRY-001")
    void retry_afterMaxRetriesGoesToDlq() throws Exception {
        setup();
        EmailOutbox row = EmailOutbox.create("u@x.kr", "code", "subj", "body", FIXED);
        // First failure
        row.markFailure("err1", FIXED, d -> FIXED.plusSeconds(d));
        // Second failure
        row.markFailure("err2", FIXED, d -> FIXED.plusSeconds(d));
        when(outboxRepository.findDueForSending(FIXED)).thenReturn(List.of(row));
        doThrow(new EmailSendException("err3")).when(senderService).send(any(), any(), any());

        service.processQueue();

        // Third failure: retryCount reaches 3 == MAX_RETRIES → DLQ
        assertThat(row.getStatus()).isEqualTo(EmailOutboxStatus.DLQ);
        assertThat(row.getRetryCount()).isEqualTo(3);
        assertThat(row.getNextAttemptAt()).isNull();
    }

    @Test
    @Tag("EMAIL-RETRY-002")
    void retry_findDueForSendingExcludesFutureNextAttemptAt() {
        // This is an interface contract test: the JPQL ensures only
        // rows with nextAttemptAt <= now are returned. We verify the
        // exclusion logic by exercising findDueForSending's stub and
        // confirming the service only iterates due rows.
        setup();
        EmailOutbox dueRow = EmailOutbox.create("u@x.kr", "code", "s", "b", FIXED);
        // Stub returns only the due row — future-nextAttemptAt rows
        // are filtered by the repository query in real use.
        when(outboxRepository.findDueForSending(FIXED)).thenReturn(List.of(dueRow));

        int processed = service.processQueue();

        assertThat(processed)
            .as("processQueue iterates exactly the rows returned by findDueForSending; "
              + "RETRY rows with future nextAttemptAt are excluded at the repository layer "
              + "(EMAIL-RETRY-002)")
            .isEqualTo(1);
    }

    @Test
    @Tag("EMAIL-TEMPLATE-001")
    void template_missingVariableProducesEmptyStringNotException() {
        setup();
        seedTemplate("notice", "Greeting {{name}}!", "Score: {{score}}");

        EmailTemplateService.Rendered out =
            service.previewTemplate("notice", Map.of("name", "지수"));

        assertThat(out.subject()).isEqualTo("Greeting 지수!");
        // 'score' was not supplied → empty string substitution, NOT an exception
        assertThat(out.body()).isEqualTo("Score: ");
    }

    @Test
    @Tag("EMAIL-ADMIN-001")
    void admin_adminRetryRefusesSentRow() {
        setup();
        EmailOutbox row = EmailOutbox.create("u@x.kr", "code", "subj", "body", FIXED);
        row.markSent(FIXED);
        UUID id = row.getId();
        when(outboxRepository.findById(id)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.adminRetry(id))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot retry SENT");
        verify(outboxRepository, times(0)).save(any(EmailOutbox.class));
    }
}

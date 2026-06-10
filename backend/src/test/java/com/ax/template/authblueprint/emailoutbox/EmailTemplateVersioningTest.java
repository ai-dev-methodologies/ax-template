package com.ax.template.authblueprint.emailoutbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.ax.template.authblueprint.common.MemberWriter;

/**
 * R60 iter1 F10 closure via Wave D2 — pins versioned-update + immutable
 * history behavior on {@link EmailTemplateService}. Matches the
 * pure-Mockito convention used by sibling email-outbox tests
 * (no Spring context, no Testcontainers).
 */
@Tag("EMAIL")
class EmailTemplateVersioningTest {

    private static final Instant FIXED = Instant.parse("2026-05-27T10:00:00Z");

    private EmailTemplateRepository templateRepository;
    private MemberWriter members;
    private EmailTemplateService service;

    @BeforeEach
    void setUp() {
        templateRepository = mock(EmailTemplateRepository.class);
        members = mock(MemberWriter.class);
        Clock clock = Clock.fixed(FIXED, ZoneOffset.UTC);
        service = new EmailTemplateService(templateRepository, members, clock);
    }

    @Test
    @Tag("EMAIL-TEMPLATE-VERSION-001")
    void upsertTemplate_new_persistsVersionOneAndInitialHistoryRow() {
        when(templateRepository.findById("welcome")).thenReturn(Optional.empty());
        when(templateRepository.save(any(EmailTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        EmailTemplate saved = service.upsertTemplate("welcome", "Hi {{name}}", "Hello {{name}}.");

        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getSubjectTemplate()).isEqualTo("Hi {{name}}");
        verify(templateRepository).save(any(EmailTemplate.class));
        verify(members, times(1)).persist(any(EmailTemplateHistory.class));
    }

    @Test
    @Tag("EMAIL-TEMPLATE-VERSION-002")
    void upsertTemplate_existing_capturesPreUpdateSnapshotAndIncrementsVersion() {
        EmailTemplate existing = new EmailTemplate("welcome", "Old subject {{name}}", "Old body", 3);
        when(templateRepository.findById("welcome")).thenReturn(Optional.of(existing));
        when(templateRepository.save(any(EmailTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        EmailTemplate saved = service.upsertTemplate("welcome", "New subject {{name}}", "New body");

        assertThat(saved.getVersion()).isEqualTo(4);
        assertThat(saved.getSubjectTemplate()).isEqualTo("New subject {{name}}");
        assertThat(saved.getBodyTemplate()).isEqualTo("New body");
        verify(members).persist(any(EmailTemplateHistory.class));
    }

    @Test
    @Tag("EMAIL-TEMPLATE-VERSION-003")
    void getTemplateAtVersion_returnsHistoricalSnapshot() {
        EmailTemplateHistory snap = new EmailTemplateHistory(
            UUID.randomUUID(), "welcome", 2, "v2 subject", "v2 body", FIXED);
        when(templateRepository.findHistoryAtVersion("welcome", 2)).thenReturn(Optional.of(snap));

        EmailTemplateHistory result = service.getTemplateAtVersion("welcome", 2);

        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo(2);
        assertThat(result.getSubjectTemplate()).isEqualTo("v2 subject");
    }

    @Test
    @Tag("EMAIL-TEMPLATE-VERSION-003B")
    void getTemplateAtVersion_missingVersion_returnsNull() {
        when(templateRepository.findHistoryAtVersion("welcome", 99)).thenReturn(Optional.empty());

        EmailTemplateHistory result = service.getTemplateAtVersion("welcome", 99);

        assertThat(result).isNull();
    }

    @Test
    @Tag("EMAIL-TEMPLATE-VERSION-004")
    void emailTemplateHistory_allBusinessColumnsAreUpdatableFalse() throws Exception {
        for (String fieldName : new String[]{"id", "templateCode", "version", "subjectTemplate", "bodyTemplate", "capturedAt"}) {
            Field f = EmailTemplateHistory.class.getDeclaredField(fieldName);
            Column col = f.getAnnotation(Column.class);
            assertThat(col)
                .as("EmailTemplateHistory#%s must carry a @Column annotation", fieldName)
                .isNotNull();
            assertThat(col.updatable())
                .as("EmailTemplateHistory#%s must be updatable=false (immutable snapshot)", fieldName)
                .isFalse();
        }
    }

    @Test
    @Tag("EMAIL-TEMPLATE-VERSION-005")
    void render_unchangedByVersioningRefactor() {
        when(templateRepository.findById("welcome"))
            .thenReturn(Optional.of(new EmailTemplate("welcome", "Hi {{name}}", "Hello {{name}}, body.")));

        EmailTemplateService.Rendered rendered = service.render(
            "welcome", Map.of("name", "Alice"));

        assertThat(rendered.subject()).isEqualTo("Hi Alice");
        assertThat(rendered.body()).isEqualTo("Hello Alice, body.");
        verify(members, never()).persist(any(EmailTemplateHistory.class));
    }
}

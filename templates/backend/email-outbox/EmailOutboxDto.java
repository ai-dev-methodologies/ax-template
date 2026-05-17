/**
 * @ax-template-meta
 * template_id: backend/email-outbox/EmailOutboxDto
 * layer: backend-domain
 * domain: email-outbox
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "JEP 395 — Records (Final, Java 16)"
 *     url: "https://openjdk.org/jeps/395"
 *   - source_type: external
 *     citation: "OWASP Mass Assignment Cheat Sheet — only expose fields the client is allowed to set"
 *     url: "https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All inner records are Java 16 records — immutable value types.
 *   Map at service layer: Response.from(EmailOutbox entity).
 */
package com.example.app.emailoutbox;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO container for the email-outbox domain.
 *
 * <p>Inner records:
 * <ul>
 *   <li>{@link Response}         — response for list/get endpoints
 *   <li>{@link PreviewRequest}   — body for POST /preview
 *   <li>{@link PreviewResponse}  — rendered subject + body
 * </ul>
 */
public final class EmailOutboxDto {

    private EmailOutboxDto() {}

    // ─── response ────────────────────────────────────────────────────────

    /**
     * Response DTO for a single outbox entry.
     */
    public record Response(
        UUID id,
        String recipient,
        String subject,
        EmailOutbox.EmailOutboxStatus status,
        int retryCount,
        Instant nextAttemptAt,
        Instant sentAt,
        String dlqReason,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static Response from(EmailOutbox e) {
            return new Response(
                e.getId(),
                e.getRecipient(),
                e.getSubject(),
                e.getStatus(),
                e.getRetryCount(),
                e.getNextAttemptAt(),
                e.getSentAt(),
                e.getDlqReason(),
                e.getCreatedAt(),
                e.getUpdatedAt()
            );
        }
    }

    // ─── preview ─────────────────────────────────────────────────────────

    /**
     * Request body for POST /api/admin/email-outbox/preview.
     */
    public record PreviewRequest(
        @NotBlank
        String templateCode,

        Map<String, Object> templateVars
    ) {}

    /**
     * Response for template preview — rendered subject + body.
     */
    public record PreviewResponse(
        String subject,
        String body
    ) {}
}

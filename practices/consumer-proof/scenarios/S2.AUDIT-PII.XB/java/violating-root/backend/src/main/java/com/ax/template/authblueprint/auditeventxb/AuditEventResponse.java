package com.ax.template.authblueprint.auditeventxb;

import java.time.Instant;

/**
 * AuditEventResponse — the FE-facing DTO. THIS IS THE VIOLATING SHAPE.
 *
 * The dogfood cell's GAP: the real catalog's AuditLogPiiRedactor masks
 * actorIp at write time, but nothing enforces that EVERY PII field on the
 * entity is redacted at the exact seam where a row becomes a response DTO.
 * Here, actorEmail crosses straight from AuditEvent.getActorEmail() into
 * the record returned to the browser — a realistic AI-generated shape
 * ("just map every field across") that leaks PII into the FE audit-log
 * viewer, browser devtools/network tab, any FE error-reporting integration
 * that captures response bodies, and this app's own client-side cache.
 */
public record AuditEventResponse(
    Long id,
    String actorId,
    String actorEmail,
    String action,
    Instant occurredAt
) {
    public static AuditEventResponse from(AuditEvent entry) {
        return new AuditEventResponse(
            entry.getId(),
            entry.getActorId(),
            entry.getActorEmail(),
            entry.getAction(),
            entry.getOccurredAt()
        );
    }
}

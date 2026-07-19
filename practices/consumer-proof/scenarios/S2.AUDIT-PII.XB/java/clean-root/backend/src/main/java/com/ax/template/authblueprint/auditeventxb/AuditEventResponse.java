package com.ax.template.authblueprint.auditeventxb;

import java.time.Instant;

/**
 * AuditEventResponse — the FE-facing DTO. CLEAN shape: actorEmail never
 * crosses the boundary raw. AuditPiiHelper.piiHash() reduces it to a short
 * stable, non-recoverable correlation token before it ever reaches the
 * record returned to the browser (mirrors the real catalog's
 * common.AuditPiiHelper#piiHash contract — R61 audit-log-pii-hash-required).
 */
public record AuditEventResponse(
    Long id,
    String actorId,
    String actorEmailHash,
    String action,
    Instant occurredAt
) {
    public static AuditEventResponse from(AuditEvent entry) {
        return new AuditEventResponse(
            entry.getId(),
            entry.getActorId(),
            AuditPiiHelper.piiHash(entry.getActorEmail()),
            entry.getAction(),
            entry.getOccurredAt()
        );
    }
}

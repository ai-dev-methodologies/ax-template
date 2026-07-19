package com.ax.template.authblueprint.auditeventxb;

import java.time.Instant;

/**
 * AuditEvent — BE-only audit record for the S2.AUDIT-PII.XB scenario slice.
 * Mirrors the shape of the real auditlog domain's AuditLog entity (actor
 * identity + action + timestamp + free-form metadata), plus one PII field
 * (actorEmail) captured at write time — the realistic AI-generated shape:
 * "let's also store the actor's email on the audit row for support lookups."
 *
 * This is the BE side of the dogfood cell's cross-boundary gap: actorEmail
 * is real PII, and nothing on the entity itself prevents it from reaching a
 * FE-facing response DTO unredacted (see AuditEventResponse).
 */
public class AuditEvent {

    private final Long id;
    private final String actorId;
    private final String actorEmail;
    private final String action;
    private final Instant occurredAt;
    private final String metadataJson;

    public AuditEvent(Long id, String actorId, String actorEmail, String action,
                       Instant occurredAt, String metadataJson) {
        this.id = id;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.occurredAt = occurredAt;
        this.metadataJson = metadataJson;
    }

    public Long getId() { return id; }
    public String getActorId() { return actorId; }
    public String getActorEmail() { return actorEmail; }
    public String getAction() { return action; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getMetadataJson() { return metadataJson; }
}

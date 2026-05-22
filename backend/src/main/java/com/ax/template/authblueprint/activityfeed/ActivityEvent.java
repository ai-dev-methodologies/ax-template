package com.ax.template.authblueprint.activityfeed;

import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * ActivityEvent — a single (actor, verb, object) record visible to a polymorphic audience.
 *
 * <p>Trace:
 * <ul>
 *   <li>ACT-PUBLISH-001/002 — actorUserId stamped server-side; audience defaults to actor</li>
 *   <li>ACT-PUBLISH-003 — UNIQUE(actor_user_id, idempotency_key) backs idempotent publish</li>
 *   <li>ACT-READ-001 / ACT-AUTHZ-002/003 — visibility = (actor OR audience contains caller)</li>
 * </ul>
 */
@Entity
@Table(
    name = "activity_events",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_activity_events_actor_idempotency",
        columnNames = {"actor_user_id", "idempotency_key"}
    ),
    indexes = {
        @Index(name = "ix_activity_events_actor_created", columnList = "actor_user_id,created_at"),
        @Index(name = "ix_activity_events_object", columnList = "object_type,object_id"),
        @Index(name = "ix_activity_events_created", columnList = "created_at")
    }
)
public class ActivityEvent {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "actor_user_id", nullable = false, updatable = false, length = 255)
    private String actorUserId;

    @Column(name = "verb", nullable = false, updatable = false, length = 64)
    private String verb;

    @Column(name = "object_type", nullable = false, updatable = false, length = 64)
    private String objectType;

    @Column(name = "object_id", nullable = false, updatable = false, length = 255)
    private String objectId;

    @Column(name = "subject_type", updatable = false, length = 64)
    private String subjectType;

    @Column(name = "subject_id", updatable = false, length = 255)
    private String subjectId;

    @Column(name = "metadata_json", updatable = false, length = 4096)
    private String metadataJson;

    @Column(name = "idempotency_key", updatable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Basic(fetch = FetchType.EAGER)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "activity_event_audience",
        joinColumns = @JoinColumn(name = "event_id"),
        indexes = @Index(name = "ix_activity_event_audience_user", columnList = "audience_user_id,event_id")
    )
    @Column(name = "audience_user_id", nullable = false, length = 255)
    private Set<String> audienceUserIds = new HashSet<>();

    protected ActivityEvent() {}

    private ActivityEvent(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.actorUserId = b.actorUserId;
        this.verb = b.verb;
        this.objectType = b.objectType;
        this.objectId = b.objectId;
        this.subjectType = b.subjectType;
        this.subjectId = b.subjectId;
        this.metadataJson = b.metadataJson;
        this.idempotencyKey = b.idempotencyKey;
        this.createdAt = (b.createdAt != null) ? b.createdAt : Instant.now();
        this.audienceUserIds = (b.audienceUserIds != null)
            ? new HashSet<>(b.audienceUserIds)
            : new HashSet<>();
    }

    public UUID getId() { return id; }
    public String getActorUserId() { return actorUserId; }
    public String getVerb() { return verb; }
    public String getObjectType() { return objectType; }
    public String getObjectId() { return objectId; }
    public String getSubjectType() { return subjectType; }
    public String getSubjectId() { return subjectId; }
    public String getMetadataJson() { return metadataJson; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }

    public Set<String> getAudienceUserIds() {
        return new HashSet<>(audienceUserIds);
    }

    public boolean isVisibleTo(String userId) {
        return userId != null
            && (userId.equals(actorUserId) || audienceUserIds.contains(userId));
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String actorUserId;
        private String verb;
        private String objectType;
        private String objectId;
        private String subjectType;
        private String subjectId;
        private String metadataJson;
        private String idempotencyKey;
        private Instant createdAt;
        private Set<String> audienceUserIds;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder actorUserId(String v) { this.actorUserId = v; return this; }
        public Builder verb(String v) { this.verb = v; return this; }
        public Builder objectType(String v) { this.objectType = v; return this; }
        public Builder objectId(String v) { this.objectId = v; return this; }
        public Builder subjectType(String v) { this.subjectType = v; return this; }
        public Builder subjectId(String v) { this.subjectId = v; return this; }
        public Builder metadataJson(String v) { this.metadataJson = v; return this; }
        public Builder idempotencyKey(String v) { this.idempotencyKey = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder audienceUserIds(Set<String> v) { this.audienceUserIds = v; return this; }

        public ActivityEvent build() { return new ActivityEvent(this); }
    }
}

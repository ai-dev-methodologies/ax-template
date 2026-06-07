package com.ax.template.authblueprint.announcement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * announcement-l0 entity — a time-boxed system notice. State moves only through
 * {@link AnnouncementStateMachine} (the sole mutator); there is NO public state setter
 * and NO stored is_active/is_visible flag (ANN-WINDOW-001: visibility is DERIVED at read
 * time from state==PUBLISHED AND now in [startsAt, endsAt)). id/createdBy/createdAt are
 * immutable (@Column updatable=false). Spec: specs/announcement-l0.yaml.
 */
@Entity
@Table(name = "announcements")
public class Announcement {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, length = 5000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private AnnouncementState state;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Optimistic-lock version (provider-managed; no public setter). */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Announcement() {}

    public Announcement(UUID id, String title, String body, Instant startsAt, Instant endsAt,
                        String createdBy, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.state = AnnouncementState.DRAFT;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    /** Sole mutator's hook — package-private so only AnnouncementStateMachine (same package) advances state. */
    void setState(AnnouncementState next) {
        this.state = next;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public AnnouncementState getState() { return state; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }

    /** Derived visibility (ANN-WINDOW-001): PUBLISHED and now within the half-open window. */
    public boolean isActiveAt(Instant now) {
        return state == AnnouncementState.PUBLISHED
            && !now.isBefore(startsAt)   // startsAt <= now
            && now.isBefore(endsAt);     // now < endsAt (exclusive)
    }
}

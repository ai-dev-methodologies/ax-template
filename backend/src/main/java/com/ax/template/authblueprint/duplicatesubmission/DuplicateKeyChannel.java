package com.ax.template.authblueprint.duplicatesubmission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * duplicate-submission-key-l0 root — the immutable configuration for one intake channel's
 * natural-key attribute set (subject + loss-date + loss-type, fixed by this domain's schema —
 * see DUPKEY-NATURAL-001) and its configurable secondary FUZZY window (DUPKEY-FUZZY-002).
 * {@link Submission} references this channel BY ID (cross-aggregate reference, not an object
 * pointer — a separate {@link AggregateRoot}).
 */
@AggregateRoot
@Entity
@Table(name = "duplicate_key_channels")
@Check(constraints = "fuzzy_window_days >= 0")
public class DuplicateKeyChannel {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Opaque label for the intake stream this channel governs (e.g. "auto-claims"). */
    @Column(name = "scope_label", nullable = false, updatable = false, length = 200)
    private String scopeLabel;

    /** DUPKEY-FUZZY-002 — the near-match window in days (same subject + loss-date within N days). */
    @Column(name = "fuzzy_window_days", nullable = false, updatable = false)
    private int fuzzyWindowDays;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DuplicateKeyChannel() {}

    public DuplicateKeyChannel(UUID id, String scopeLabel, int fuzzyWindowDays, Instant createdAt) {
        this.id = id;
        this.scopeLabel = scopeLabel;
        this.fuzzyWindowDays = fuzzyWindowDays;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getScopeLabel() { return scopeLabel; }
    public int getFuzzyWindowDays() { return fuzzyWindowDays; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}

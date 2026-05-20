package com.ax.template.authblueprint.featureflags;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Runtime feature toggle.
 * <p>
 * Trace:
 * <ul>
 *   <li>specs/feature-flags-l0.yaml — FF-CRUD-001..004 (CRUD persistence),
 *       FF-EVAL-001/002 (lookup by name).</li>
 *   <li>blueprints/feature-flags-manifest.yaml#backend — entity contract
 *       (name PK, enabled boolean, description varchar, createdAt, updatedAt).</li>
 * </ul>
 */
@Entity
@Table(name = "feature_flags")
public class FeatureFlag {

    @Id
    @Column(name = "name", nullable = false, length = 63, updatable = false)
    private String name;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. */
    protected FeatureFlag() {}

    private FeatureFlag(String name, boolean enabled, String description,
                        Instant createdAt, Instant updatedAt) {
        this.name = Objects.requireNonNull(name, "name");
        this.enabled = enabled;
        this.description = description;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** Factory used by {@link FeatureFlagService#create}. */
    public static FeatureFlag create(String name, boolean enabled, String description) {
        Instant now = Instant.now();
        return new FeatureFlag(name, enabled, description, now, now);
    }

    /** FF-CRUD-003 — patch enabled and/or description in place. */
    public void update(Boolean newEnabled, String newDescription) {
        if (newEnabled != null) {
            this.enabled = newEnabled;
        }
        if (newDescription != null) {
            this.description = newDescription;
        }
        this.updatedAt = Instant.now();
    }

    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

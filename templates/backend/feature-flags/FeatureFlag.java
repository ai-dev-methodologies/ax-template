/**
 * @ax-template-meta
 * template_id: backend/feature-flags/FeatureFlag
 * layer: backend-domain
 * domain: feature-flags
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Entity mapping with @Entity, @Id"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   FeatureFlag uses name (String) as the natural primary key — no UUID needed.
 *   Hard-delete strategy: no soft-delete to avoid zombie flags accumulating.
 */
package com.example.app.featureflags;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

/**
 * FeatureFlag entity — runtime-controlled boolean toggle.
 *
 * <p>Name is the natural PK (no UUID): flag names are unique, immutable, and
 * human-readable. Hard-delete strategy prevents zombie flag accumulation.
 *
 * <p>Evaluation is fail-closed: unknown flags always return active=false.
 * See FeatureFlagService.isActive().
 *
 * <p>spec_ref: specs/feature-flags-l0.yaml (FF-EVAL-001, FF-EVAL-002, FF-CRUD-001..FF-CRUD-004)
 */
@Entity
@Table(name = "feature_flags")
@EntityListeners(AuditingEntityListener.class)
public class FeatureFlag {

    /**
     * Natural PK — lowercase, hyphen-separated, 2–63 chars.
     * Pattern: ^[a-z][a-z0-9-]{1,62}$
     */
    @Id
    @Column(name = "name", nullable = false, length = 63)
    private String name;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "description", length = 500)
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FeatureFlag() {}

    public FeatureFlag(String name, boolean enabled, String description) {
        this.name = name;
        this.enabled = enabled;
        this.description = description;
    }

    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setDescription(String description) { this.description = description; }
}

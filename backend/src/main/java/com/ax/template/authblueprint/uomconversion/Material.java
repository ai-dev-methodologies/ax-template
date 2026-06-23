package com.ax.template.authblueprint.uomconversion;

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
 * dimensional-uom-conversion-l0 root (UOMCONV-MATERIAL-001). A material carries the bridging
 * property a CROSS-dimension conversion needs — its density (mass per unit volume) and/or
 * unit-weight (mass per unit count) — as a VERSIONED chain of {@link MaterialProperty} members.
 * The root holds only the CURRENT version pointer; each property version is an immutable member
 * row appended through {@link com.ax.template.authblueprint.common.MemberWriter}. Lifecycle moves
 * ONLY via the package-private hook, called by {@link UomConversionService} under the material's
 * PESSIMISTIC_WRITE row lock so a concurrent correction cannot interleave (UOMCONV-VERSION-001).
 */
@AggregateRoot
@Entity
@Table(name = "uom_materials")
@Check(constraints = "current_version >= 0")
public class Material {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The material's external reference (SKU / item code) — opaque, recorded verbatim. */
    @Column(name = "material_ref", nullable = false, updatable = false, length = 200)
    private String materialRef;

    /** The highest appended property version; 0 means no bridge recorded yet. */
    @Column(name = "current_version", nullable = false)
    private long currentVersion;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Material() {}

    public Material(UUID id, String materialRef, Instant createdAt) {
        this.id = id;
        this.materialRef = materialRef;
        this.currentVersion = 0L;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — advance the current property version pointer (UOMCONV-VERSION-001).
     *  The prior versions are preserved as append-only member rows; this only moves the pointer. */
    long advanceVersion() {
        return ++this.currentVersion;
    }

    public UUID getId() { return id; }
    public String getMaterialRef() { return materialRef; }
    public long getCurrentVersion() { return currentVersion; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}

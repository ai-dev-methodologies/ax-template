package com.ax.template.authblueprint.optlock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

/**
 * optimistic-locking-l0 reference entity — a mutable, owner-scoped resource carrying a JPA
 * {@code @Version} optimistic-lock column (OPTLOCK-VERSION-001). The version is provider-managed
 * (incremented at flush); application code NEVER sets it, and it is NOT a client-writable DTO
 * field. The strong ETag (OPTLOCK-ETAG-001) is derived from {@code (id, version)} via
 * {@code common.OptimisticLockingSupport.etag}. Spec: specs/optimistic-locking-l0.yaml.
 */
@Entity
@Table(name = "optlock_resources",
        indexes = @Index(name = "ix_optlock_owner", columnList = "owner_id"))
public class OptlockResource {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false, length = 255)
    private String ownerId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Provider-managed optimistic-lock version (OPTLOCK-VERSION-001). No public setter. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected OptlockResource() {}

    public OptlockResource(UUID id, String ownerId, String name, int quantity) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /** Current version (0 before first flush). Read-only — the provider owns writes. */
    public long getVersion() {
        return version == null ? 0L : version;
    }
}

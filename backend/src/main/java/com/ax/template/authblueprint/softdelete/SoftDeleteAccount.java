package com.ax.template.authblueprint.softdelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * soft-delete-l0 reference parent entity — a tombstoned, owner-scoped aggregate root with a
 * natural key ({@code email}) that is unique only among LIVE rows (SOFTDELETE-UNIQUE-001, enforced
 * by a partial unique index). {@code deletedAt} is the tombstone (NULL = live); it is set
 * server-side and only cleared via the explicit restore path.
 *
 * <p>Default-excluding reads are achieved with explicit {@code ...AndDeletedAtIsNull} repository
 * finders rather than {@code @SQLRestriction} — restore / admin / purge MUST read tombstoned rows,
 * which an entity-wide SQL restriction would hide. Spec: specs/soft-delete-l0.yaml.
 */
@Entity
@Table(name = "soft_delete_accounts",
        indexes = @Index(name = "ix_sda_owner", columnList = "owner_id"))
public class SoftDeleteAccount {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false, length = 255)
    private String ownerId;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Tombstone: NULL = live, non-NULL = soft-deleted. Server-set only (SOFTDELETE-MARK-001). */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected SoftDeleteAccount() {}

    public SoftDeleteAccount(UUID id, String ownerId, String email, String name) {
        this.id = id;
        this.ownerId = ownerId;
        this.email = email;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** SOFTDELETE-MARK-001 — tombstone at a server-supplied UTC instant. */
    void markDeleted(Instant at) {
        this.deletedAt = at;
    }

    /** SOFTDELETE-RESTORE-001 — the only path that clears the tombstone. */
    void clearDeleted() {
        this.deletedAt = null;
    }
}

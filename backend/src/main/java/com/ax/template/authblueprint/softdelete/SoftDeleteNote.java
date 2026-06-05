package com.ax.template.authblueprint.softdelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * soft-delete-l0 reference CHILD entity — a dependent of {@link SoftDeleteAccount}. Cascade
 * soft-delete tombstones it together with its parent in one transaction (SOFTDELETE-CASCADE-001);
 * restore cascades symmetrically ONLY to children tombstoned at-or-after the parent's
 * {@code deletedAt} (a child deleted earlier on its own stays deleted).
 *
 * <p>Spec: specs/soft-delete-l0.yaml#SOFTDELETE-CASCADE-001.
 */
@Entity
@Table(name = "soft_delete_notes",
        indexes = @Index(name = "ix_sdn_account", columnList = "account_id"))
public class SoftDeleteNote {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "text", nullable = false, length = 1000)
    private String text;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected SoftDeleteNote() {}

    public SoftDeleteNote(UUID id, UUID accountId, String text) {
        this.id = id;
        this.accountId = accountId;
        this.text = text;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getText() {
        return text;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    void markDeleted(Instant at) {
        this.deletedAt = at;
    }

    void clearDeleted() {
        this.deletedAt = null;
    }
}

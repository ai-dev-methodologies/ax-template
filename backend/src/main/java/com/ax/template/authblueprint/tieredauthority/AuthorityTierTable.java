package com.ax.template.authblueprint.tieredauthority;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * AuthorityTierTable — one immutable, versioned snapshot of the 전결 규정 (amount-tiered
 * decision authority) config (ATA-BOUNDARY-001 / ATA-SNAPSHOT-001). Reconfiguring the table
 * NEVER edits an existing version — it inserts a NEW row with {@code tableVersion = max + 1}.
 * This is what lets {@link TieredDecisionRecord} snapshot "which table version" a decision was
 * evaluated against and have that stay true forever, even after a later reconfiguration.
 */
@AggregateRoot
@Entity
@Table(name = "authority_tier_tables")
public class AuthorityTierTable {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "table_version", nullable = false, updatable = false, unique = true)
    private int tableVersion;

    @Column(name = "created_by", nullable = false, updatable = false, length = 200)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuthorityTierTable() {}

    public AuthorityTierTable(UUID id, int tableVersion, String createdBy, Instant createdAt) {
        this.id = id;
        this.tableVersion = tableVersion;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public int getTableVersion() { return tableVersion; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}

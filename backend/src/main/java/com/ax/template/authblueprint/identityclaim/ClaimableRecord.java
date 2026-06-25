package com.ax.template.authblueprint.identityclaim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Represents a record created anonymously (by a guest/session key) that can be
 * CLAIMED by an authenticated user on first auth.
 *
 * <p>Invariant traces:
 * <ul>
 *   <li>IDCLAIM-CLAIM-001 — all unclaimed records for a claimKey transfer in ONE transaction.</li>
 *   <li>IDCLAIM-IDEMPOTENT-001 — ownership transfer is atomic CAS; replays match 0 null rows → no-op.</li>
 *   <li>IDCLAIM-GUARD-001 — ownerUserId NEVER writable via a public setter; transfer ONLY via
 *       the atomic {@code UPDATE ... WHERE owner_user_id IS NULL} query on the repository.</li>
 * </ul>
 *
 * <p>There is intentionally NO public setter for {@code ownerUserId}. Ownership transfer is
 * structurally enforced by the atomic compare-and-set JPQL in
 * {@link ClaimableRecordRepository#claimUnowned}, which atomically sets the owner only when
 * the row is unclaimed (owner_user_id IS NULL). This prevents CWE-367 TOCTOU races.
 */
@AggregateRoot
@Entity
@Table(name = "claimable_records")
public class ClaimableRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The anonymous guest key (e.g. guest email or session id) under which the record
     * was created before authentication. Immutable once set.
     */
    @Column(name = "claim_key", updatable = false, nullable = false)
    private String claimKey;

    /**
     * NULL = unclaimed/anonymous. Set ONLY via the atomic CAS query — never via a setter.
     * (IDCLAIM-GUARD-001)
     */
    @Column(name = "owner_user_id")
    private String ownerUserId;

    /** Trivial payload — demonstrates the pattern without domain logic. */
    @Column(name = "label", nullable = false)
    private String label;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ClaimableRecord() {}

    static ClaimableRecord create(String claimKey, String label) {
        ClaimableRecord r = new ClaimableRecord();
        r.id = UUID.randomUUID();
        r.claimKey = claimKey;
        r.ownerUserId = null;
        r.label = label;
        return r;
    }

    public UUID getId() { return id; }
    public String getClaimKey() { return claimKey; }
    public String getOwnerUserId() { return ownerUserId; }
    public String getLabel() { return label; }

    // ownerUserId intentionally has NO public setter — IDCLAIM-GUARD-001.
}

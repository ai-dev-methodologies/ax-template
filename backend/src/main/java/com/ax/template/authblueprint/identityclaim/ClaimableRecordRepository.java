package com.ax.template.authblueprint.identityclaim;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClaimableRecordRepository extends JpaRepository<ClaimableRecord, UUID> {

    /**
     * Atomic compare-and-set: transfers all unclaimed records for {@code claimKey}
     * to {@code userId} in ONE statement — only rows where owner_user_id IS NULL
     * are updated. Returns the count of rows actually updated.
     *
     * <p>IDCLAIM-CLAIM-001 — atomicity: this single UPDATE is the exclusive write path.
     * <p>IDCLAIM-IDEMPOTENT-001 — idempotency: a replay finds 0 NULL rows → count 0.
     * <p>IDCLAIM-GUARD-001 — guard: rows already owned (owner_user_id IS NOT NULL) are
     * never touched, enforced structurally by the WHERE clause (CWE-367 TOCTOU prevented).
     */
    /**
     * clearAutomatically=true — evicts updated rows from the first-level cache so a
     * subsequent same-transaction read sees the new ownerUserId, not the stale snapshot.
     * flushAutomatically=true — flushes pending JPA writes before the bulk UPDATE runs,
     * preventing a race between a pending INSERT and this UPDATE for the same claimKey.
     * (Follows dispatch/ProviderRepository convention.)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ClaimableRecord r SET r.ownerUserId = :userId WHERE r.claimKey = :claimKey AND r.ownerUserId IS NULL")
    int claimUnowned(@Param("claimKey") String claimKey, @Param("userId") String userId);

    /** Find all records with the given claimKey (for test inspection). */
    List<ClaimableRecord> findByClaimKey(String claimKey);

    /** Find all records owned by the given user (for test inspection). */
    List<ClaimableRecord> findByOwnerUserId(String ownerUserId);
}

package com.ax.template.authblueprint.identityclaim;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.ax.template.authblueprint.identityclaim.IdentityClaimDtos.ClaimResult;

/**
 * Sole mutator for the identityclaim domain.
 *
 * <p>Invariant traces:
 * <ul>
 *   <li>IDCLAIM-CLAIM-001 — {@link #claimOnFirstAuth} executes ONE atomic UPDATE in a single
 *       @Transactional — all matching unclaimed records transfer or none do.</li>
 *   <li>IDCLAIM-IDEMPOTENT-001 — a replayed claim finds 0 unclaimed rows → count 0, no-op.</li>
 *   <li>IDCLAIM-GUARD-001 — ownership transfer is via the CAS query ONLY; no hand-set path exists.</li>
 * </ul>
 */
@Service
public class IdentityClaimService {

    private final ClaimableRecordRepository repository;

    public IdentityClaimService(ClaimableRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Persists an anonymous record (ownerUserId=null) under the given claimKey.
     */
    @Transactional
    public ClaimableRecord addAnonymousRecord(String claimKey, String label) {
        return repository.save(ClaimableRecord.create(claimKey, label));
    }

    /**
     * Claims all unclaimed records matching {@code claimKey} for {@code userId} in one
     * atomic UPDATE (IDCLAIM-CLAIM-001). Returns the number of records actually claimed.
     * A replay or concurrent duplicate call returns 0 (IDCLAIM-IDEMPOTENT-001).
     * Records already owned by a different user are structurally protected
     * (IDCLAIM-GUARD-001 — WHERE owner_user_id IS NULL).
     */
    @Transactional
    public ClaimResult claimOnFirstAuth(String claimKey, String userId) {
        int claimed = repository.claimUnowned(claimKey, userId);
        return new ClaimResult(claimed);
    }

    /**
     * Records for {@code claimKey} VISIBLE to the caller: unclaimed (ownerUserId IS NULL — visible
     * pre-claim) or owned by the caller (visible post-claim). Records owned by a DIFFERENT principal
     * are filtered out — the domain's cross-principal leak-prevention thesis (IDCLAIM-GUARD-001).
     */
    @Transactional(readOnly = true)
    public List<ClaimableRecord> recordsVisibleTo(String claimKey, String callerId) {
        return repository.findByClaimKey(claimKey).stream()
            .filter(r -> r.getOwnerUserId() == null || r.getOwnerUserId().equals(callerId))
            .toList();
    }
}

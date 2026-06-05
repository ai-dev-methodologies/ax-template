package com.ax.template.authblueprint.optlock;

import com.ax.template.authblueprint.common.OptimisticLockingSupport;
import com.ax.template.authblueprint.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * optimistic-locking-l0 transactional core. The {@link #update} read-modify-write runs in ONE
 * transaction so the If-Match precondition (428/412 via {@link OptimisticLockingSupport}) and the
 * {@code @Version} flush-race (409, surfaced as {@code ObjectOptimisticLockingFailureException})
 * are both inside the lock window. {@code saveAndFlush} forces the version bump before the caller
 * derives the new ETag (the bug the support class exists to prevent).
 *
 * <p>Spec: specs/optimistic-locking-l0.yaml#OPTLOCK-VERSION-001 / -CONFLICT-001 / -LOSTUPDATE-001.
 */
@Service
public class OptlockService {

    /**
     * Read-modify-write window held INSIDE the transaction so two concurrent writers that both
     * passed the If-Match check race the flush deterministically — exactly one wins, the other's
     * stale {@code @Version} UPDATE bumps zero rows → 409 (OPTLOCK-LOSTUPDATE-001 keystone). A real
     * handler's own work provides this window; the reference makes it explicit.
     */
    static final long WRITE_WINDOW_MS = 150;

    private final OptlockResourceRepository repository;

    public OptlockService(OptlockResourceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OptlockResource create(String owner, String name, int quantity) {
        return repository.saveAndFlush(new OptlockResource(UUID.randomUUID(), owner, name, quantity));
    }

    @Transactional(readOnly = true)
    public OptlockResource get(UUID id, String owner) {
        return repository.findByIdAndOwnerId(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("optlock resource not found"));
    }

    @Transactional
    public OptlockResource update(UUID id, String owner, String ifMatch, String name, int quantity) {
        OptlockResource current = repository.findByIdAndOwnerId(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("optlock resource not found"));
        // 428 (If-Match absent) / 412 (stale validator) — before any mutation. The resourceKey folds
        // the entityType in so the comparison ETag matches the controller's 3-part emitted ETag.
        OptimisticLockingSupport.requireMatch(ifMatch, resourceKey(id), current.getVersion());
        current.setName(name);
        current.setQuantity(quantity);
        widenRaceWindow();
        return repository.saveAndFlush(current); // flush bumps @Version; concurrent stale flush → 409
    }

    /**
     * The strong-ETag resourceId for an optlock resource: {@code "<entityType>-<id>"}. The support
     * helper appends {@code "-<version>"}, yielding the OPTLOCK-ETAG-001 3-part validator
     * {@code "<entityType>-<id>-<version>"}. Shared by the controller (emit) and this service (compare).
     */
    static String resourceKey(UUID id) {
        return OptlockMetrics.RESOURCE + "-" + id;
    }

    private static void widenRaceWindow() {
        try {
            Thread.sleep(WRITE_WINDOW_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

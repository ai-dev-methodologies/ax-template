package com.ax.template.authblueprint.rangeownership;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lazily find-or-creates the singleton {@link RangeRegistryLock} row in its OWN committed
 * transaction. A real deployment seeds the row via the Flyway migration; integration tests run
 * on {@code ddl-auto=create-drop} (entity-derived schema only — docs/NEW-DOMAIN-CHECKLIST.md
 * item 7), where the migration's INSERT never executes. {@code REQUIRES_NEW} is deliberate: a
 * caught constraint-violation during flush can otherwise leave the CALLING transaction's
 * persistence context unusable — isolating the seed attempt in its own transaction means a lost
 * creation race is a harmless no-op, never a poisoned caller transaction. A separate bean (not a
 * self-invoked method on {@link RangeOwnershipService}) deliberately, to avoid the Spring
 * self-invocation trap — mirrors {@code RejectedAttemptRecorder}'s rationale.
 */
@Component
public class RangeRegistryLockInitializer {

    private final RangeRegistryLockRepository lock;

    public RangeRegistryLockInitializer(RangeRegistryLockRepository lock) {
        this.lock = lock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureExists() {
        if (lock.findById(RangeRegistryLock.GLOBAL_ID).isEmpty()) {
            try {
                lock.saveAndFlush(new RangeRegistryLock(RangeRegistryLock.GLOBAL_ID));
            } catch (DataIntegrityViolationException raceLost) {
                // another concurrent caller already seeded it — fine.
            }
        }
    }
}

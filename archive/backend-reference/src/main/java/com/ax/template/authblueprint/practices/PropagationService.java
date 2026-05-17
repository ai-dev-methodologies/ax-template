package com.ax.template.authblueprint.practices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Fixtures for PRACTICES-TX-004: REQUIRES_NEW suspends the outer transaction and runs
 * the body in its own. Audit logs, side-channel writes, and any commit that must persist
 * regardless of the caller's success use REQUIRES_NEW; everything else stays on the
 * default REQUIRED.
 */
@Service
public class PropagationService {

    @Transactional(propagation = Propagation.REQUIRED)
    public String requiredTxName() {
        return TransactionSynchronizationManager.getCurrentTransactionName();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String requiresNewTxName() {
        return TransactionSynchronizationManager.getCurrentTransactionName();
    }
}

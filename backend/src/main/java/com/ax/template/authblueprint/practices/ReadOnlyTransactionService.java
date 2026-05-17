package com.ax.template.authblueprint.practices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Fixtures for PRACTICES-TX-002: read-only transaction marker.
 * `@Transactional(readOnly = true)` lets the persistence layer skip dirty-checking and
 * lets the JDBC driver hint at read-only access (which a replica router can take advantage
 * of). Forgetting the flag is silent — the query still runs, but with full overhead.
 */
@Service
public class ReadOnlyTransactionService {

    @Transactional(readOnly = true)
    public boolean inReadOnlyTx() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    }

    @Transactional
    public boolean inReadWriteTx() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    }
}

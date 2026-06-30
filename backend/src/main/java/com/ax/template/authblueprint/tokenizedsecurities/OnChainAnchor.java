package com.ax.template.authblueprint.tokenizedsecurities;

import java.util.List;

/**
 * On-chain anchor seam (ANCHOR-001/002).
 * Default impl: InMemoryAnchor (write-through test-double; no real chain dependency).
 * A fork replaces this with the real chain client (ERC-3643 / distributed ledger SDK).
 */
public interface OnChainAnchor {

    /**
     * Anchor the transfer intent and return a NON-NULL, NON-BLANK tx reference.
     * Called inside the transfer transaction — the ref is stored immutably on the TransferEntry.
     * Implementations MUST NOT return null or blank; returning blank is a contract violation
     * and the caller (SecurityTokenRegisterService) will throw IllegalStateException to prevent
     * a silently un-anchored entry from being committed.
     */
    String anchor(AnchorIntent intent);

    /** Return all anchor records for the given tokenCode (for reconciliation). */
    List<AnchorRecord> recordsFor(String tokenCode);
}

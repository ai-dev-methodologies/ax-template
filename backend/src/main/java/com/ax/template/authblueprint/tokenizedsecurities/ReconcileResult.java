package com.ax.template.authblueprint.tokenizedsecurities;

import java.util.List;

/**
 * Result of an on-chain/off-chain reconciliation (ANCHOR-002).
 * converged=true means every register entry has a matching anchor record and vice versa.
 * breaks lists the transferIds that diverge (missing on one side or units mismatch).
 */
public record ReconcileResult(boolean converged, List<String> breaks) {}

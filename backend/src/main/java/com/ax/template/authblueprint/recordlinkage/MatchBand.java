package com.ax.template.authblueprint.recordlinkage;

/**
 * record-linkage-l0 Fellegi-Sunter bands (LINK-BAND-001). AUTO_MATCH = at/above the upper
 * threshold (merges unattended, trail identical). REVIEW = between the thresholds — the
 * clerical-review class: only an explicit human confirm/reject decides. NO_MATCH = below
 * the lower threshold (not confirmable; re-propose after the records change).
 */
public enum MatchBand {
    AUTO_MATCH,
    REVIEW,
    NO_MATCH
}

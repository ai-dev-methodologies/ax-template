package com.ax.template.authblueprint.dunning;

/**
 * dunning-collections-l0 aging buckets (DUNNING-AGING-001). Classified DETERMINISTICALLY from
 * days-overdue at a RECORDED as-of instant: CURRENT (≤ 0), B1_30 (1–30), B2_60 (31–60),
 * B3_90_PLUS (61+). The bucket feeds the allowance-for-doubtful-accounts the receivable is
 * stated net of (17 CFR 210.5-02(4)).
 */
public enum AgingBucket {
    CURRENT,
    B1_30,
    B2_60,
    B3_90_PLUS;

    /** Deterministic classification by whole days overdue (as-of minus due date). */
    public static AgingBucket of(long daysOverdue) {
        if (daysOverdue <= 0) {
            return CURRENT;
        }
        if (daysOverdue <= 30) {
            return B1_30;
        }
        if (daysOverdue <= 60) {
            return B2_60;
        }
        return B3_90_PLUS;
    }
}

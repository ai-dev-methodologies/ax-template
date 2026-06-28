package com.ax.template.authblueprint.offereligibility;

/**
 * The deterministic, recorded reason an offer was applied or NOT-applied to an order.
 * Exactly one reason is produced per evaluation. {@link #ELIGIBLE} is the only applied
 * outcome; every other value is a fail-closed NOT-applied outcome.
 */
public enum EligibilityReason {

    /** All gates passed — the offer is applicable. */
    ELIGIBLE,

    /** QUALIFIER-MINQTY gate: the qualifying lines did not meet the declared minimum quantity. */
    QUALIFIER_MIN_QTY_NOT_MET,

    /** SEGMENT-ELIGIBILITY gate: the customer is neither on the allow-list nor in the matched segment. */
    CUSTOMER_NOT_ELIGIBLE,

    /** No order line matches the declared target criteria — there is nothing to discount. */
    NO_TARGET_LINE,

    /** FAIL-CLOSED: the offer declares no qualifier criteria (neither SKU nor tag). */
    MISSING_QUALIFIER_CRITERIA,

    /** FAIL-CLOSED: the offer declares no target criteria (neither SKU nor tag). */
    MISSING_TARGET_CRITERIA,

    /** FAIL-CLOSED: the offer declares no eligibility criteria (empty allow-list AND no segment). */
    MISSING_ELIGIBILITY_CRITERIA,

    /** FAIL-CLOSED: the evaluation context carries no resolvable customer identity. */
    UNKNOWN_CUSTOMER
}

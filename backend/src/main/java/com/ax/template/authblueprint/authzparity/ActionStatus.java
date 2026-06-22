package com.ax.template.authblueprint.authzparity;

/**
 * authorization-parity-l0 action lifecycle (AUTHZPARITY-ENVELOPE/EXEC-001): an action is
 * AUTHORIZED into an envelope, then either EXECUTED (parity matched, four-eyes + gates all
 * satisfied) or BLOCKED (a parity-mismatch attempt was refused — the action stays admissible
 * for a correct retry, so BLOCKED is NOT terminal; the blocked ATTEMPT is what is recorded
 * immutably). There is no delete path — an action is a permanent authorization of record.
 */
public enum ActionStatus {
    AUTHORIZED,
    EXECUTED
}

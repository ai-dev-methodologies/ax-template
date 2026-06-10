package com.ax.template.authblueprint.decisiongov;

/**
 * decision-governance-l0 version kinds. COMPUTED = the first system determination.
 * RECOMPUTED = a re-determination (new basis, mandatory reason). OVERRIDE = a human
 * substitution (mandatory justification + four-eyes approver ≠ requester).
 */
public enum DecisionKind {
    COMPUTED,
    RECOMPUTED,
    OVERRIDE
}

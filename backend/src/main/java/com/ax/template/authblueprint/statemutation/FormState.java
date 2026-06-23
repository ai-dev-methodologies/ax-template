package com.ax.template.authblueprint.statemutation;

/**
 * state-conditional-mutability-l0 lifecycle (STATEMUTATION-MONOTONE-001). A GovernedForm walks
 * DRAFT → SUBMITTED → APPROVED → LOCKED forward; each forward step SHRINKS the mutable-field-set
 * ({@link StateFieldPolicy}). A widening (re-open back to DRAFT) is an explicit RECORDED governed
 * transition, never a silent unlock. LOCKED is terminal. The legal-edge graph lives on
 * {@link GovernedFormStateMachine} (the sole status mutator), not here.
 */
public enum FormState {
    DRAFT,
    SUBMITTED,
    APPROVED,
    LOCKED
}

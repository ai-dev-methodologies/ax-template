package com.ax.template.authblueprint.reconciliation;

/**
 * external-reconciliation-l0 human disposition of a BREAK (RECON-DISPOSE-001). ACCEPT_INTERNAL
 * (the internal value stands), ACCEPT_EXTERNAL (the external value stands), ADJUST (a corrective
 * entry is warranted). A disposition records who/when/reason and is the gate a run must clear on
 * every break before it can be RESOLVED (RECON-RESOLVE-001).
 */
public enum DispositionType {
    ACCEPT_INTERNAL,
    ACCEPT_EXTERNAL,
    ADJUST
}

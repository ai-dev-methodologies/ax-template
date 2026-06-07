package com.ax.template.authblueprint.netting;

/**
 * collection-conservation-l0 netting-run lifecycle. OPEN = accepting gross obligations; NETTED =
 * terminal — the reduction has run once, positions are computed, no further obligation or re-net.
 * A correction is a NEW run (NET-INPUTS-IMMUTABLE-001), never a transition out of NETTED.
 */
public enum NettingStatus {
    OPEN,
    NETTED
}

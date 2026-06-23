package com.ax.template.authblueprint.netmetering;

/**
 * signed-dual-register-l0 reading directions. IMPORT = energy drawn FROM the grid (the + side of the
 * signed net); EXPORT = energy fed BACK to the grid (the − side). Each direction is an independently
 * value-monotone register (RFC 2578 Counter); the meter's signed NET = cumulativeImport − cumulativeExport.
 */
public enum MeterDirection {
    IMPORT,
    EXPORT
}

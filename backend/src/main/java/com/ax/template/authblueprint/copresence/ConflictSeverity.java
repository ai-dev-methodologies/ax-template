package com.ax.template.authblueprint.copresence;

/**
 * negative-copresence-gate-l0 finding severity. ABSOLUTE = hard-stop (the write MUST NOT commit under
 * any reason). RELATIVE = soft-stop (blocks by default, may be overridden with an atomic mandatory
 * reason, GATE-OVERRIDE-001).
 */
public enum ConflictSeverity {
    ABSOLUTE,
    RELATIVE
}

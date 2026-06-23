package com.ax.template.authblueprint.reproducibility;

/**
 * reproducible-procedure-l0 procedure kinds. A DRAW records a seed + algorithm + selected ids
 * (PROC-DRAW-001 / PROC-REPLAY-001); a CLASSIFICATION records an input hash + classifier version
 * + resolved class (PROC-CLASS-001). Both are auditable deterministic procedures.
 */
public enum ProcedureKind {
    DRAW,
    CLASSIFICATION
}

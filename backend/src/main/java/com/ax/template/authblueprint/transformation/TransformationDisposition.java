package com.ax.template.authblueprint.transformation;

/**
 * Governed, CLOSED disposition vocabulary for a residual leg (XFORM-RESIDUAL-CLASSIFIED-001).
 * Every residual unit MUST carry one of these — there is deliberately NO "miscellaneous" bucket,
 * so the sum of the classified categories equals the total residual and every lost unit is
 * attributable (accounted loss, never silent shrinkage).
 */
public enum TransformationDisposition {
    SCRAP,
    REWORK,
    YIELD_LOSS,
    WIP_REMAINDER
}

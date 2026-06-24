package com.ax.template.authblueprint.quorumresolution;

/**
 * Controls how abstentions factor into the threshold base.
 * EXCLUDE_FROM_BASE: base = yes + no (abstentions ignored in comparison).
 * COUNT_AS_NO: abstentions added to the no weight before comparison.
 */
public enum AbstentionMode {
    EXCLUDE_FROM_BASE,
    COUNT_AS_NO
}

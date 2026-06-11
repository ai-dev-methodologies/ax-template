package com.ax.template.authblueprint.recordlinkage;

/**
 * record-linkage-l0 record lifecycle: ACTIVE, or MERGED — tombstoned with a forward pointer
 * to the survivor, values retained verbatim, NEVER deleted (LINK-SURVIVOR-001).
 */
public enum RecordStatus {
    ACTIVE,
    MERGED
}

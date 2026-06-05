package com.ax.template.authblueprint.softdelete;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * G4 — VIOLATION proof tests for the soft-delete reference workload.
 * Structural invariants that would re-open the data-loss / leaked-tombstone
 * surface if relaxed (specs/soft-delete-l0.yaml). Plain reflection, no Spring
 * context, @Tag bound to testSoftDelete.
 */
@Tag("SOFT_DELETE")
class SoftDeleteViolationProofTest {

    @Test
    void violation_tombstoneFieldExistsAndIsInstant() throws Exception {
        // SOFTDELETE-MARK-001: deletion is a tombstone (deletedAt), never a
        // physical DELETE. The column must exist and be a timestamp (NULL=live).
        Field deletedAt = SoftDeleteAccount.class.getDeclaredField("deletedAt");
        assertThat(deletedAt.getType())
                .as("soft-delete tombstone must be an Instant (NULL = live)")
                .isEqualTo(Instant.class);
    }

    @Test
    void violation_noPublicTombstoneSetter() {
        // The tombstone is server-set only (via the service mark/restore path);
        // a public setDeletedAt(...) would let a caller forge or clear deletion.
        boolean hasPublicSetter = false;
        for (Method m : SoftDeleteAccount.class.getMethods()) {
            if (m.getName().equals("setDeletedAt")) {
                hasPublicSetter = true;
            }
        }
        assertThat(hasPublicSetter)
                .as("deletedAt must have no public setter (server-set only)")
                .isFalse();
    }

    @Test
    void violation_ownerIdImmutable() throws Exception {
        // owner_id is updatable=false: a soft-deleted row must not be re-owned.
        Field ownerId = SoftDeleteAccount.class.getDeclaredField("ownerId");
        jakarta.persistence.Column col = ownerId.getAnnotation(jakarta.persistence.Column.class);
        assertThat(col).isNotNull();
        assertThat(col.updatable()).isFalse();
    }
}

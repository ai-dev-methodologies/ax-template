package com.ax.template.authblueprint.optlock;

import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * G4 — VIOLATION proof tests for the optimistic-locking reference workload.
 * Structural invariants that would re-open the lost-update surface if relaxed
 * (specs/optimistic-locking-l0.yaml). Mirrors the R31..R36 ViolationProofTest
 * convention: plain reflection, no Spring context, @Tag bound to testOptimisticLocking.
 */
@Tag("OPTLOCK")
class OptlockResourceViolationProofTest {

    @Test
    void violation_versionFieldIsJpaManaged() throws Exception {
        // OPTLOCK-VERSION-001: the version column MUST be a JPA @Version field so
        // the provider increments it at flush and a stale write collides.
        Field version = OptlockResource.class.getDeclaredField("version");
        assertThat(version.isAnnotationPresent(Version.class))
                .as("version field must carry @Version (provider-managed optimistic lock)")
                .isTrue();
        assertThat(version.getType()).isEqualTo(Long.class);
    }

    @Test
    void violation_noPublicVersionSetter() {
        // The version is provider-managed; application code MUST NOT set it. A
        // public setVersion(...) would let a caller forge the lock token.
        boolean hasPublicVersionSetter = false;
        for (Method m : OptlockResource.class.getMethods()) {
            if (m.getName().equals("setVersion")) {
                hasPublicVersionSetter = true;
            }
        }
        assertThat(hasPublicVersionSetter)
                .as("version must have no public setter (provider-managed only)")
                .isFalse();
    }

    @Test
    void violation_idIsImmutable() throws Exception {
        // The identity column is updatable=false; a mutable id would let an
        // owner-scoped resource be re-keyed under another owner.
        Field id = OptlockResource.class.getDeclaredField("id");
        jakarta.persistence.Column col = id.getAnnotation(jakarta.persistence.Column.class);
        assertThat(col).isNotNull();
        assertThat(col.updatable()).isFalse();
    }
}

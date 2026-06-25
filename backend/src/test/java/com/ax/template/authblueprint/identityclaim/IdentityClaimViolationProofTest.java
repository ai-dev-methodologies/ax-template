package com.ax.template.authblueprint.identityclaim;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Violation proof tests for identity-claim-on-auth-l0.yaml (IDCLAIM-GUARD-001).
 *
 * <p>Plain reflection — no Spring context required. Proves that {@link ClaimableRecord}
 * exposes NO public setter for {@code ownerUserId}, making hand-assignment of ownership
 * structurally impossible. Ownership transfer is only via the atomic CAS query
 * in {@link ClaimableRecordRepository#claimUnowned}.
 */
@Tag("IDENTITY_CLAIM")
class IdentityClaimViolationProofTest {

    @Test
    @Tag("IDCLAIM-GUARD-001")
    void violation_noPublicSetterForOwnerUserId() {
        for (Method m : ClaimableRecord.class.getDeclaredMethods()) {
            if ("setOwnerUserId".equals(m.getName())) {
                assertThat(Modifier.isPublic(m.getModifiers()))
                    .as("ClaimableRecord.setOwnerUserId MUST NOT be public — "
                      + "ownership transfer must only happen via the atomic CAS query "
                      + "(IDCLAIM-GUARD-001). A public setter would allow callers to bypass "
                      + "the WHERE owner_user_id IS NULL guard and commit a CWE-367 TOCTOU race.")
                    .isFalse();
            }
        }

        // Also assert no public method starts with 'setOwnerUserId'.
        long publicSetterCount = java.util.Arrays.stream(ClaimableRecord.class.getDeclaredMethods())
            .filter(m -> m.getName().startsWith("setOwnerUserId") && Modifier.isPublic(m.getModifiers()))
            .count();
        assertThat(publicSetterCount)
            .as("ClaimableRecord must have zero public setOwnerUserId* methods (IDCLAIM-GUARD-001)")
            .isZero();
    }
}

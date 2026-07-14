package com.ax.template.identityverification;

import com.ax.template.authblueprint.identityverification.IdentityVerificationService;
import com.ax.template.authblueprint.identityverification.VerifiedIdentity;

import jakarta.persistence.Column;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R54 — VIOLATION proof tests for identity-verification residual closure.
 * Mirrors R31..R36 / R51 convention. Structural invariants that would re-open
 * the 개인정보보호법 §24 surface if relaxed.
 */
@Tag("IDENTITY_VERIFICATION")
class VerifiedIdentityViolationProofTest {

    @Test
    void violation_noRrnField() {
        // IDV-CALLBACK-003: VerifiedIdentity entity MUST NOT have ANY field
        // whose name encodes a resident registration number. Banning the
        // *field name* (not just the value) keeps a future refactor from
        // re-introducing the column with a renamed accessor.
        for (Field f : VerifiedIdentity.class.getDeclaredFields()) {
            String name = f.getName().toLowerCase(Locale.ROOT);
            assertThat(name)
                .as("VerifiedIdentity field name must not be RRN-shaped: " + f.getName())
                .doesNotContain("rrn")
                .doesNotContain("residentregistrationnumber")
                .doesNotContain("주민번호")
                .doesNotContain("주민등록번호")
                .doesNotContain("socialsecuritynumber");
        }
    }

    @Test
    void violation_immutableContentColumns() {
        // IDV-CALLBACK-002: the row MUST reflect the original provider
        // response. Re-attributing a verified row to a different CI / DI /
        // name / dob / verifiedAt / providerName would falsify the §24 audit
        // trail. Every content column is @Column(updatable=false).
        for (String name : new String[] {
                "ci", "di", "name", "dob", "verifiedAt", "providerName" }) {
            Field f;
            try {
                f = VerifiedIdentity.class.getDeclaredField(name);
            } catch (NoSuchFieldException nf) {
                throw new AssertionError(
                    "VerifiedIdentity is missing required field: " + name, nf);
            }
            Column c = f.getAnnotation(Column.class);
            assertThat(c)
                .as("VerifiedIdentity." + name + " missing @Column")
                .isNotNull();
            assertThat(c.updatable())
                .as("VerifiedIdentity." + name
                  + " MUST be @Column(updatable=false) — verified rows are an "
                  + "immutable audit record of what the provider returned")
                .isFalse();
        }
    }

    @Test
    void violation_noPublicSetters() {
        // The entity is created via the static factory create(data); no public
        // setters may exist. A setter would let any caller mutate a verified
        // identity post-persist, breaking IDV-CALLBACK-002 immutability.
        for (var m : VerifiedIdentity.class.getDeclaredMethods()) {
            String name = m.getName();
            if (name.startsWith("set")) {
                assertThat(Modifier.isPublic(m.getModifiers()))
                    .as("VerifiedIdentity." + name + " must NOT be public — "
                      + "the entity is immutable after create(...)")
                    .isFalse();
            }
        }
    }

    @Test
    void violation_auditActionConstantUnchanged() {
        // IDV-AUDIT-001 — the audit action token is a public contract. Dashboards
        // and SIEM pipelines filter on this exact string. Changing it would
        // silently drop existing alerts.
        assertThat(IdentityVerificationService.AUDIT_ACTION)
            .as("IdentityVerificationService.AUDIT_ACTION is the IDV-AUDIT-001 "
              + "contract token — downstream filters depend on this string")
            .isEqualTo("IDENTITY_VERIFICATION_CALLBACK");
        assertThat(IdentityVerificationService.RESOURCE_TYPE)
            .isEqualTo("verified_identity");
    }

    @Test
    void violation_concordanceAuditActionIsDistinctFromCallbackAction() {
        // IDV-CONCORDANCE-001 — the mismatch audit MUST use its OWN action constant,
        // never folded into the generic per-callback AUDIT_ACTION.
        assertThat(IdentityVerificationService.CONCORDANCE_AUDIT_ACTION)
            .as("the concordance-mismatch audit action must be distinct from the callback audit action")
            .isNotEqualTo(IdentityVerificationService.AUDIT_ACTION)
            .isEqualTo("IDENTITY_VERIFICATION_CONCORDANCE_MISMATCH");
    }
}

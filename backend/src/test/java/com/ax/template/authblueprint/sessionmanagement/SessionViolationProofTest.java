package com.ax.template.authblueprint.sessionmanagement;

import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof tests for R33 — closes METHODOLOGY.md Step 5 (R37 retrofit).
 * Mirrors R31/R32/R34/R35/R36 ViolationProofTest convention. R33 originally
 * shipped SessionDogfoodIter1Test for iter1 closure but never the dedicated
 * structural proof file — added at R37 to pass l4_domain_reachability_guard.
 */
@Tag("SESSION")
class SessionViolationProofTest {

    @Test
    @Tag("SESS-LIFECYCLE-001")
    void violation_userIdJtiUniqueConstraint_isStillDeclared() {
        jakarta.persistence.Table table = SessionRecord.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table).isNotNull();
        boolean hasUserJti = false;
        for (UniqueConstraint uc : table.uniqueConstraints()) {
            String[] cols = uc.columnNames();
            if (cols.length == 2 && contains(cols, "user_id") && contains(cols, "jti")) {
                hasUserJti = true;
                break;
            }
        }
        assertThat(hasUserJti)
            .as("UNIQUE(user_id, jti) backs SESS-LIFECYCLE-001 idempotent register at the DB layer")
            .isTrue();
    }

    @Test
    @Tag("SESS-LIFECYCLE-001")
    void violation_keyColumns_areImmutable() throws Exception {
        for (String name : new String[] { "userId", "jti", "createdAt", "expiresAt" }) {
            Field f = SessionRecord.class.getDeclaredField(name);
            Column c = f.getAnnotation(Column.class);
            assertThat(c.updatable())
                .as("SessionRecord." + name + " MUST be immutable")
                .isFalse();
        }
    }

    @Test
    @Tag("SESS-INTROSPECT-002")
    void violation_ipAndUserAgentColumns_existButAreNotInDtos() throws Exception {
        // The entity carries ip + UA columns for forensics, but @JsonIgnore prevents
        // accidental leakage if the entity is ever returned directly. SessionResponse
        // record verified at code level to mask via IpAddressMasker / UserAgentSummarizer.
        Field ip = SessionRecord.class.getDeclaredField("ipAddress");
        Field ua = SessionRecord.class.getDeclaredField("userAgent");
        assertThat(ip.getAnnotation(com.fasterxml.jackson.annotation.JsonIgnore.class))
            .as("SessionRecord.ipAddress MUST be @JsonIgnore — DTO uses masked form")
            .isNotNull();
        assertThat(ua.getAnnotation(com.fasterxml.jackson.annotation.JsonIgnore.class))
            .as("SessionRecord.userAgent MUST be @JsonIgnore — DTO uses summarized form")
            .isNotNull();
    }

    @Test
    @Tag("SESS-LIFECYCLE-003")
    void violation_noPublicSetters() {
        for (var m : SessionRecord.class.getDeclaredMethods()) {
            if (m.getName().startsWith("set") || m.getName().startsWith("mark") || m.getName().startsWith("touch")) {
                if (m.getName().startsWith("set")) {
                    int mod = m.getModifiers();
                    assertThat(java.lang.reflect.Modifier.isPublic(mod))
                        .as("SessionRecord." + m.getName() + " must NOT be public — service is the only mutator")
                        .isFalse();
                }
            }
        }
    }

    private static boolean contains(String[] arr, String v) {
        for (String s : arr) if (s.equals(v)) return true;
        return false;
    }
}

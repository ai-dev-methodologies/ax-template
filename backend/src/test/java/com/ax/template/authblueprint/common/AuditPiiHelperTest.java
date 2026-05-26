package com.ax.template.authblueprint.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R60 dogfood iter1 — AuditPiiHelper unit coverage.
 *
 * <p>Pins the contract that the helper hashes recipients deterministically
 * and that sanitizeReason redacts every PII pattern in the catalog deny-list.
 * If a future refactor weakens either invariant, these tests fail loud.
 */
@Tag("EMAIL")
class AuditPiiHelperTest {

    @Test
    void piiHash_emptyOrNull_returnsPlaceholder() {
        assertThat(AuditPiiHelper.piiHash(null)).isEqualTo("(none)");
        assertThat(AuditPiiHelper.piiHash("")).isEqualTo("(none)");
        assertThat(AuditPiiHelper.piiHash("  ")).isEqualTo("(none)");
    }

    @Test
    void piiHash_deterministicLength16Hex() {
        String h = AuditPiiHelper.piiHash("user@example.com");
        assertThat(h).hasSize(16);
        assertThat(h).matches("^[0-9a-f]{16}$");
        // determinism: same input → same hash
        assertThat(AuditPiiHelper.piiHash("user@example.com")).isEqualTo(h);
    }

    @Test
    void piiHash_differentInputsDifferentOutputs() {
        String a = AuditPiiHelper.piiHash("a@example.com");
        String b = AuditPiiHelper.piiHash("b@example.com");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void sanitizeReason_redactsKoreanRrn() {
        String s = AuditPiiHelper.sanitizeReason("SMTP error: user 901231-1234567 rejected");
        assertThat(s).doesNotContain("901231-1234567");
        assertThat(s).contains("[REDACTED]");
    }

    @Test
    void sanitizeReason_redactsKoreanMobile() {
        assertThat(AuditPiiHelper.sanitizeReason("error to 010-1234-5678 failed")).contains("[REDACTED]");
        assertThat(AuditPiiHelper.sanitizeReason("error to 01012345678 failed")).contains("[REDACTED]");
    }

    @Test
    void sanitizeReason_redactsJwtShape() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY";
        String s = AuditPiiHelper.sanitizeReason("sender adapter said: " + jwt);
        assertThat(s).doesNotContain(jwt);
        assertThat(s).contains("[REDACTED]");
    }

    @Test
    void sanitizeReason_redactsBearerHeader() {
        String s = AuditPiiHelper.sanitizeReason("Authorization: Bearer abc123def456ghi");
        assertThat(s).doesNotContain("abc123def456ghi");
    }

    @Test
    void sanitizeReason_redactsEmailAddress() {
        String s = AuditPiiHelper.sanitizeReason("rejected: target@example.com is not authorized");
        assertThat(s).doesNotContain("target@example.com");
        assertThat(s).contains("[REDACTED]");
    }

    @Test
    void sanitizeReason_redactsIpv4() {
        String s = AuditPiiHelper.sanitizeReason("connection refused from 192.168.1.100");
        assertThat(s).doesNotContain("192.168.1.100");
    }

    @Test
    void sanitizeReason_redactsInternalHostname() {
        assertThat(AuditPiiHelper.sanitizeReason("smtp-relay.internal timeout")).contains("[REDACTED]");
        assertThat(AuditPiiHelper.sanitizeReason("mailer.local refused")).contains("[REDACTED]");
    }

    @Test
    void sanitizeReason_nullInputReturnsNull() {
        assertThat(AuditPiiHelper.sanitizeReason(null)).isNull();
    }

    @Test
    void sanitizeReason_innocuousMessagePreserved() {
        // R60 invariant — sanitize must NOT mangle innocuous prose. An ops
        // engineer reading the audit needs to see "connection refused" / "550
        // policy denied" / "queue full" etc. unchanged.
        String s = AuditPiiHelper.sanitizeReason("550 mailbox quota exceeded");
        assertThat(s).contains("550 mailbox quota exceeded");
    }
}

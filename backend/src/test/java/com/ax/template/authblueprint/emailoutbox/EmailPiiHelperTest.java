package com.ax.template.authblueprint.emailoutbox;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R60 dogfood iter1 — EmailPiiHelper unit coverage.
 *
 * <p>Pins the contract that the helper hashes recipients deterministically
 * and that sanitizeReason redacts every PII pattern in the catalog deny-list.
 * If a future refactor weakens either invariant, these tests fail loud.
 */
@Tag("EMAIL")
class EmailPiiHelperTest {

    @Test
    void piiHash_emptyOrNull_returnsPlaceholder() {
        assertThat(EmailPiiHelper.piiHash(null)).isEqualTo("(none)");
        assertThat(EmailPiiHelper.piiHash("")).isEqualTo("(none)");
        assertThat(EmailPiiHelper.piiHash("  ")).isEqualTo("(none)");
    }

    @Test
    void piiHash_deterministicLength16Hex() {
        String h = EmailPiiHelper.piiHash("user@example.com");
        assertThat(h).hasSize(16);
        assertThat(h).matches("^[0-9a-f]{16}$");
        // determinism: same input → same hash
        assertThat(EmailPiiHelper.piiHash("user@example.com")).isEqualTo(h);
    }

    @Test
    void piiHash_differentInputsDifferentOutputs() {
        String a = EmailPiiHelper.piiHash("a@example.com");
        String b = EmailPiiHelper.piiHash("b@example.com");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @SuppressWarnings("deprecation")
    void recipientHash_deprecatedAlias_delegatesToPiiHash() {
        // R62 — recipientHash kept as @Deprecated alias for fork-receivers
        // who already wired it. Pinning the equivalence ensures the alias
        // doesn't accidentally drift from piiHash.
        String input = "user@example.com";
        assertThat(EmailPiiHelper.recipientHash(input)).isEqualTo(EmailPiiHelper.piiHash(input));
    }

    @Test
    void sanitizeReason_redactsKoreanRrn() {
        String s = EmailPiiHelper.sanitizeReason("SMTP error: user 901231-1234567 rejected");
        assertThat(s).doesNotContain("901231-1234567");
        assertThat(s).contains("[REDACTED]");
    }

    @Test
    void sanitizeReason_redactsKoreanMobile() {
        assertThat(EmailPiiHelper.sanitizeReason("error to 010-1234-5678 failed")).contains("[REDACTED]");
        assertThat(EmailPiiHelper.sanitizeReason("error to 01012345678 failed")).contains("[REDACTED]");
    }

    @Test
    void sanitizeReason_redactsJwtShape() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY";
        String s = EmailPiiHelper.sanitizeReason("sender adapter said: " + jwt);
        assertThat(s).doesNotContain(jwt);
        assertThat(s).contains("[REDACTED]");
    }

    @Test
    void sanitizeReason_redactsBearerHeader() {
        String s = EmailPiiHelper.sanitizeReason("Authorization: Bearer abc123def456ghi");
        assertThat(s).doesNotContain("abc123def456ghi");
    }

    @Test
    void sanitizeReason_redactsEmailAddress() {
        String s = EmailPiiHelper.sanitizeReason("rejected: target@example.com is not authorized");
        assertThat(s).doesNotContain("target@example.com");
        assertThat(s).contains("[REDACTED]");
    }

    @Test
    void sanitizeReason_redactsIpv4() {
        String s = EmailPiiHelper.sanitizeReason("connection refused from 192.168.1.100");
        assertThat(s).doesNotContain("192.168.1.100");
    }

    @Test
    void sanitizeReason_redactsInternalHostname() {
        assertThat(EmailPiiHelper.sanitizeReason("smtp-relay.internal timeout")).contains("[REDACTED]");
        assertThat(EmailPiiHelper.sanitizeReason("mailer.local refused")).contains("[REDACTED]");
    }

    @Test
    void sanitizeReason_nullInputReturnsNull() {
        assertThat(EmailPiiHelper.sanitizeReason(null)).isNull();
    }

    @Test
    void sanitizeReason_innocuousMessagePreserved() {
        // R60 invariant — sanitize must NOT mangle innocuous prose. An ops
        // engineer reading the audit needs to see "connection refused" / "550
        // policy denied" / "queue full" etc. unchanged.
        String s = EmailPiiHelper.sanitizeReason("550 mailbox quota exceeded");
        assertThat(s).contains("550 mailbox quota exceeded");
    }
}

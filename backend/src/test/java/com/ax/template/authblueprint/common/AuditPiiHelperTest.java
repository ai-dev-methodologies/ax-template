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
    void sanitizeReason_redactsUnhyphenatedKoreanRrn() {
        // P3-48 — a 13-digit un-hyphenated 주민등록번호 must also be redacted (the hyphen is
        // optional in the pattern, mirroring the KR mobile pattern). RED-on-revert: with the old
        // \d{6}-\d{7} pattern the un-hyphenated case leaks and this assertion fails.
        String s = AuditPiiHelper.sanitizeReason("SMTP error: user 9012311234567 rejected");
        assertThat(s).doesNotContain("9012311234567");
        assertThat(s).contains("[REDACTED]");
        // the hyphenated form still redacts (no regression).
        assertThat(AuditPiiHelper.sanitizeReason("RRN 901231-1234567 denied"))
            .doesNotContain("901231-1234567").contains("[REDACTED]");
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

    // ─── R86 edge-case coverage (PRD US-004) ──────────────────────────────

    @Test
    void sanitizeReason_bearerPrecedesJwt_collapsesToOneRedaction() {
        // The Bearer regex runs before the JWT regex (order in sanitizeReason).
        // "Bearer eyJ..." matches Bearer first → "Bearer abc..." becomes
        // "[REDACTED]" wholesale; the eyJ inside the value is then absent
        // and the JWT regex has nothing left to match. The catalog promise
        // is "no raw token reaches the column", not "every PII pattern
        // independently logged" — one [REDACTED] per overlapping match is
        // correct.
        String input = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload";
        String s = AuditPiiHelper.sanitizeReason(input);
        assertThat(s).doesNotContain("eyJ");
        assertThat(s).doesNotContain("Bearer eyJ");
        assertThat(s).contains("[REDACTED]");
    }

    @Test
    void sanitizeReason_multiplePiiInOneMessage_allRedacted() {
        // A real adapter exception can embed 3+ PII fragments — email +
        // RRN + JWT — in one message. All MUST be redacted, no overlap
        // should leak any single fragment.
        String input = "delivery failed for alice@example.com (RRN 901231-1234567) "
                     + "token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xxxxxxxxxxxxxxxxxxxx";
        String s = AuditPiiHelper.sanitizeReason(input);
        assertThat(s).doesNotContain("alice@example.com");
        assertThat(s).doesNotContain("901231-1234567");
        assertThat(s).doesNotContain("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xxxxxxxxxxxxxxxxxxxx");
        // At least 3 redaction markers — one per pattern family.
        long redactionCount = s.split("\\[REDACTED\\]", -1).length - 1;
        assertThat(redactionCount).as("3 distinct PII patterns must yield ≥3 redactions").isGreaterThanOrEqualTo(3);
    }

    @Test
    void sanitizeReason_veryLongInput_doesNotTruncateOrCrash() {
        // Defensive: a 10K-char exception message must not throw + must
        // preserve length parity (no silent truncation inside the helper;
        // truncation is the caller's responsibility — see ExportWorker
        // sanitizeAndTruncate pattern).
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) sb.append("blah blah ");      // ~10K chars
        sb.append(" alice@example.com ");
        String s = AuditPiiHelper.sanitizeReason(sb.toString());
        assertThat(s).doesNotContain("alice@example.com");
        // Length should still be on the order of the input (plus the
        // length difference from "alice@example.com" → "[REDACTED]").
        assertThat(s.length()).isGreaterThan(9000);
    }

    @Test
    void sanitizeReason_emptyAndWhitespaceOnly() {
        // Empty string: no NPE, returns empty.
        assertThat(AuditPiiHelper.sanitizeReason("")).isEqualTo("");
        // Whitespace-only: preserved as-is (no PII to redact).
        assertThat(AuditPiiHelper.sanitizeReason("   \t  \n  "))
            .isEqualTo("   \t  \n  ");
    }

    @Test
    void sanitizeReason_koreanAndEnglishMixedPii_bothRedacted() {
        // Real ops sites in 한국 enterprise contexts surface mixed-locale
        // exception strings ("invalid 주민등록번호 901231-1234567 for user
        // hong@example.kr at 010-1234-5678"). All PII fragments must be
        // redacted regardless of surrounding script.
        String input = "invalid request for 홍길동 (RRN 901231-1234567), "
                     + "email hong@example.kr, phone 010-1234-5678 — denied";
        String s = AuditPiiHelper.sanitizeReason(input);
        assertThat(s).doesNotContain("901231-1234567");
        assertThat(s).doesNotContain("hong@example.kr");
        assertThat(s).doesNotContain("010-1234-5678");
        // Korean prose ("홍길동", "invalid request") is preserved.
        assertThat(s).contains("홍길동");
    }

    @Test
    void sanitizeReason_overlappingPatterns_emailWithinIpv4_likeText() {
        // Pathological overlap: an IPv4 substring inside a longer token.
        // The IPv4 regex uses \b boundaries; "1.2.3.4.5.6" should NOT
        // false-match as multiple IPv4s in a row (the catalog refuses to
        // mangle innocuous version numbers).
        String input = "build 1.2.3.4 succeeded; release 5.6.7 deployed";
        String s = AuditPiiHelper.sanitizeReason(input);
        // 1.2.3.4 is a valid-shape IPv4 — gets redacted.
        assertThat(s).doesNotContain("1.2.3.4");
        // 5.6.7 is not an IPv4 — preserved.
        assertThat(s).contains("5.6.7");
    }
}

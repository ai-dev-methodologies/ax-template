package com.ax.template.authblueprint.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for the IMW5 consent-support primitives
 * ({@link ConsentRecord} append-only ledger row + {@link ConsentGate} derived
 * decision). Closes the zero-code gap of {@code consent-management-l0} that the
 * IDW4 EMR dogfood found SPEC-ONLY (all 3 personas hand-rolled it) and proves the
 * CONSENT-RECORD-001 contradiction resolution: current consent is DERIVED from an
 * append-only ledger, never read from a mutable column.
 *
 * <p>Framework-light: pure {@code List<ConsentRecord>} assertions, no Spring
 * context / no JPA. The {@code @Tag("COMMON_CONSENT")} is UPPERCASE per the
 * test_tag_naming_convention_guard contract.
 */
@Tag("COMMON_CONSENT")
class ConsentSupportTest {

    private static final String SUBJECT = "subject-1";
    private static final String MARKETING = "marketing_email";
    private static final String ANALYTICS = "analytics";

    // ─── absence-of-record → no consent (CONSENT-RECORD-001) ──────────────

    @Test
    void activeConsent_emptyLedger_isFalse() {
        assertThat(ConsentGate.activeConsent(SUBJECT, MARKETING, List.of())).isFalse();
    }

    @Test
    void activeConsent_nullLedger_isFalse() {
        assertThat(ConsentGate.activeConsent(SUBJECT, MARKETING, null)).isFalse();
    }

    @Test
    void activeConsent_blankSubjectOrPurpose_isFalse() {
        var ledger = List.of(ConsentRecord.grant(SUBJECT, MARKETING));
        assertThat(ConsentGate.activeConsent("", MARKETING, ledger)).isFalse();
        assertThat(ConsentGate.activeConsent(SUBJECT, " ", ledger)).isFalse();
    }

    // ─── grant → active (CONSENT-CAPTURE-001) ─────────────────────────────

    @Test
    void activeConsent_singleGrant_isTrue() {
        var ledger = List.of(ConsentRecord.grant(SUBJECT, MARKETING));
        assertThat(ConsentGate.activeConsent(SUBJECT, MARKETING, ledger)).isTrue();
    }

    // ─── withdrawal APPENDS and supersedes (CONSENT-WITHDRAW-001) ──────────

    @Test
    void activeConsent_withdrawAfterGrant_isFalse_byTimestamp() {
        Instant t0 = Instant.parse("2026-05-30T10:00:00Z");
        Instant t1 = Instant.parse("2026-05-30T11:00:00Z");
        var ledger = List.of(
                ConsentRecord.of(SUBJECT, MARKETING, ConsentRecord.Action.GRANT, t0),
                ConsentRecord.of(SUBJECT, MARKETING, ConsentRecord.Action.WITHDRAW, t1));
        // The append-only WITHDRAW supersedes the earlier GRANT; the GRANT row is
        // still present (never updated/deleted) — current state is a derived query.
        assertThat(ConsentGate.activeConsent(SUBJECT, MARKETING, ledger)).isFalse();
        assertThat(ledger).hasSize(2);
    }

    @Test
    void activeConsent_regrantAfterWithdraw_isTrue() {
        Instant t0 = Instant.parse("2026-05-30T10:00:00Z");
        Instant t1 = Instant.parse("2026-05-30T11:00:00Z");
        Instant t2 = Instant.parse("2026-05-30T12:00:00Z");
        var ledger = List.of(
                ConsentRecord.of(SUBJECT, MARKETING, ConsentRecord.Action.GRANT, t0),
                ConsentRecord.of(SUBJECT, MARKETING, ConsentRecord.Action.WITHDRAW, t1),
                ConsentRecord.of(SUBJECT, MARKETING, ConsentRecord.Action.GRANT, t2));
        assertThat(ConsentGate.activeConsent(SUBJECT, MARKETING, ledger)).isTrue();
        assertThat(ledger).hasSize(3);
    }

    // ─── purpose isolation (CONSENT-PURPOSE-001) ──────────────────────────

    @Test
    void activeConsent_isPerPurpose_notGlobal() {
        var ledger = List.of(
                ConsentRecord.grant(SUBJECT, MARKETING),
                ConsentRecord.withdraw(SUBJECT, ANALYTICS));
        assertThat(ConsentGate.activeConsent(SUBJECT, MARKETING, ledger)).isTrue();
        assertThat(ConsentGate.activeConsent(SUBJECT, ANALYTICS, ledger)).isFalse();
    }

    @Test
    void activeConsent_otherSubjectGrant_doesNotLeak() {
        var ledger = List.of(ConsentRecord.grant("other-subject", MARKETING));
        assertThat(ConsentGate.activeConsent(SUBJECT, MARKETING, ledger)).isFalse();
    }

    // ─── same-instant tie → fail-closed (WITHDRAW wins) ───────────────────

    @Test
    void activeConsent_sameInstantGrantAndWithdraw_failsClosed() {
        Instant t = Instant.parse("2026-05-30T10:00:00Z");
        var ledger = List.of(
                ConsentRecord.of(SUBJECT, MARKETING, ConsentRecord.Action.GRANT, t),
                ConsentRecord.of(SUBJECT, MARKETING, ConsentRecord.Action.WITHDRAW, t));
        assertThat(ConsentGate.activeConsent(SUBJECT, MARKETING, ledger)).isFalse();
    }

    // ─── requireConsent → 403-mapped signal ───────────────────────────────

    @Test
    void requireConsent_active_doesNotThrow() {
        var ledger = List.of(ConsentRecord.grant(SUBJECT, MARKETING));
        assertThatCode(() -> ConsentGate.requireConsent(SUBJECT, MARKETING, ledger))
                .doesNotThrowAnyException();
    }

    @Test
    void requireConsent_absent_throwsConsentRequired_carryingPurpose() {
        assertThatThrownBy(() -> ConsentGate.requireConsent(SUBJECT, MARKETING, List.of()))
                .isInstanceOf(ConsentGate.ConsentRequiredException.class)
                .hasMessageContaining(MARKETING)
                .satisfies(ex -> assertThat(
                        ((ConsentGate.ConsentRequiredException) ex).purpose()).isEqualTo(MARKETING));
    }

    // ─── ledger row is an append-only fact, not a setter-bearing entity ───

    @Test
    void consentRecord_factoriesStampActionAndFields() {
        var grant = ConsentRecord.grant(SUBJECT, MARKETING);
        assertThat(grant.action()).isEqualTo(ConsentRecord.Action.GRANT);
        assertThat(grant.subjectId()).isEqualTo(SUBJECT);
        assertThat(grant.purpose()).isEqualTo(MARKETING);
        assertThat(grant.recordedAt()).isNotNull();

        var withdraw = ConsentRecord.withdraw(SUBJECT, MARKETING);
        assertThat(withdraw.action()).isEqualTo(ConsentRecord.Action.WITHDRAW);
    }

    @Test
    void consentRecord_rejectsBlankIdentifiers() {
        assertThatThrownBy(() -> ConsentRecord.grant("", MARKETING))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsentRecord.grant(SUBJECT, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void consentRecord_hasNoPublicSetters() {
        long publicSetters = java.util.Arrays.stream(ConsentRecord.class.getMethods())
                .filter(m -> m.getName().startsWith("set"))
                .count();
        assertThat(publicSetters).isZero();
    }
}

package com.ax.template.authblueprint.common;

import com.ax.template.authblueprint.common.OptimisticLockingSupport.Decision;
import com.ax.template.authblueprint.common.OptimisticLockingSupport.PreconditionFailedException;
import com.ax.template.authblueprint.common.OptimisticLockingSupport.PreconditionOutcome;
import com.ax.template.authblueprint.common.OptimisticLockingSupport.PreconditionRequiredException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit coverage for {@link OptimisticLockingSupport} — closes the zero-code gap
 * of the {@code optimistic-locking-l0} spec (specs/optimistic-locking-l0.yaml).
 *
 * <p>Pins the three contract surfaces a fork-receiver relies on: strong ETag
 * derivation (OPTLOCK-ETAG-001), If-Match parsing, and the 428/412/match
 * precondition decisions (OPTLOCK-IFMATCH-001 / OPTLOCK-CONFLICT-001).
 * Framework-clean test: no Spring context, runs under the default {@code test}
 * task.
 */
class OptimisticLockingSupportTest {

    // ─── ETag derivation (OPTLOCK-ETAG-001) ───────────────────────────────

    @Test
    void etag_isStrongQuotedAndVersionDerived() {
        String tag = OptimisticLockingSupport.etag("42", 7);
        assertThat(tag).isEqualTo("\"42-7\"");
        // strong validator: no weak W/ prefix
        assertThat(tag).doesNotStartWith("W/");
    }

    @Test
    void etag_sameStateIsByteIdentical() {
        assertThat(OptimisticLockingSupport.etag("p1", 3))
            .isEqualTo(OptimisticLockingSupport.etag("p1", 3));
    }

    @Test
    void etag_versionBumpChangesValidator() {
        assertThat(OptimisticLockingSupport.etag("p1", 3))
            .isNotEqualTo(OptimisticLockingSupport.etag("p1", 4));
    }

    @Test
    void etag_numericOverloadMatchesStringForm() {
        assertThat(OptimisticLockingSupport.etag(42L, 7))
            .isEqualTo(OptimisticLockingSupport.etag("42", 7));
    }

    @Test
    void etag_blankResourceIdRejected() {
        assertThatThrownBy(() -> OptimisticLockingSupport.etag("  ", 1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OptimisticLockingSupport.etag((String) null, 1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── If-Match parsing ─────────────────────────────────────────────────

    @Test
    void parseIfMatch_stripsQuotes() {
        assertThat(OptimisticLockingSupport.parseIfMatch("\"42-7\"")).isEqualTo("42-7");
    }

    @Test
    void parseIfMatch_nullOrBlankReturnsNull() {
        assertThat(OptimisticLockingSupport.parseIfMatch(null)).isNull();
        assertThat(OptimisticLockingSupport.parseIfMatch("")).isNull();
        assertThat(OptimisticLockingSupport.parseIfMatch("   ")).isNull();
    }

    @Test
    void parseIfMatch_wildcardPreserved() {
        assertThat(OptimisticLockingSupport.parseIfMatch("*")).isEqualTo("*");
    }

    @Test
    void parseIfMatch_weakValidatorIsNull() {
        // RFC 7232 §3.2: a weak validator can never match an If-Match (strong comparison), so it is
        // not a usable strong tag — parseIfMatch returns null (decide() then treats it as 412 STALE).
        assertThat(OptimisticLockingSupport.parseIfMatch("W/\"42-7\"")).isNull();
    }

    @Test
    void parseIfMatch_toleratesUnquotedToken() {
        assertThat(OptimisticLockingSupport.parseIfMatch("42-7")).isEqualTo("42-7");
    }

    // ─── Precondition decision: MISSING → 428 (OPTLOCK-IFMATCH-001) ────────

    @Test
    void decide_absentIfMatch_isMissing() {
        PreconditionOutcome outcome = OptimisticLockingSupport.decide(null, "42", 7);
        assertThat(outcome.decision()).isEqualTo(Decision.MISSING);
        assertThat(outcome.currentEtag()).isEqualTo("\"42-7\"");
    }

    @Test
    void requireMatch_absentIfMatch_throws428SignalWithCurrentEtag() {
        assertThatThrownBy(() -> OptimisticLockingSupport.requireMatch(null, "42", 7))
            .isInstanceOf(PreconditionRequiredException.class)
            .extracting(ex -> ((PreconditionRequiredException) ex).currentEtag())
            .isEqualTo("\"42-7\"");
    }

    // ─── Precondition decision: STALE → 412 (OPTLOCK-CONFLICT-001) ─────────

    @Test
    void decide_staleValidator_isStaleWithAuthoritativeEtag() {
        // client holds version 6; current is 7
        PreconditionOutcome outcome = OptimisticLockingSupport.decide("\"42-6\"", "42", 7);
        assertThat(outcome.decision()).isEqualTo(Decision.STALE);
        assertThat(outcome.currentEtag()).isEqualTo("\"42-7\"");
    }

    @Test
    void requireMatch_staleValidator_throws412SignalCarryingCurrentEtag() {
        assertThatThrownBy(() -> OptimisticLockingSupport.requireMatch("\"42-6\"", "42", 7))
            .isInstanceOf(PreconditionFailedException.class)
            .extracting(ex -> ((PreconditionFailedException) ex).currentEtag())
            .isEqualTo("\"42-7\"");
    }

    @Test
    void decide_wildcardNotHonouredByDefault_isStale() {
        // optlock_allow_wildcard_ifmatch defaults false → "*" does not match a concrete validator
        PreconditionOutcome outcome = OptimisticLockingSupport.decide("*", "42", 7);
        assertThat(outcome.decision()).isEqualTo(Decision.STALE);
    }

    // ─── Precondition decision: MATCHED → proceed ─────────────────────────

    @Test
    void decide_matchingValidator_isMatched() {
        PreconditionOutcome outcome = OptimisticLockingSupport.decide("\"42-7\"", "42", 7);
        assertThat(outcome.decision()).isEqualTo(Decision.MATCHED);
    }

    @Test
    void requireMatch_matchingValidator_doesNotThrow() {
        assertThatCode(() -> OptimisticLockingSupport.requireMatch("\"42-7\"", "42", 7))
            .doesNotThrowAnyException();
    }

    @Test
    void requireMatch_weakValidatorIsStale412() {
        // RFC 7232 §3.2 — a weak validator, even one whose tag equals the current strong ETag, can
        // NEVER satisfy an If-Match strong comparison: it MUST be rejected as stale (412), carrying
        // the authoritative current ETag, NOT silently accepted as a match.
        assertThatThrownBy(() -> OptimisticLockingSupport.requireMatch("W/\"42-7\"", "42", 7))
            .isInstanceOf(PreconditionFailedException.class)
            .extracting(ex -> ((PreconditionFailedException) ex).currentEtag())
            .isEqualTo("\"42-7\"");
    }

    // ─── Stable RFC 9457 type URIs (spec-anchored constants) ──────────────

    @Test
    void problemTypeUris_matchSpec() {
        assertThat(OptimisticLockingSupport.TYPE_PRECONDITION_REQUIRED)
            .isEqualTo("urn:problem:precondition-required");
        assertThat(OptimisticLockingSupport.TYPE_PRECONDITION_FAILED)
            .isEqualTo("urn:problem:precondition-failed");
    }
}

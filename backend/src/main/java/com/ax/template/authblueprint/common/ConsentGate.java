package com.ax.template.authblueprint.common;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * IMW5 (IDW4 EMR-lite dogfood, 2026-05-30) — the DERIVED current-consent decision
 * over an append-only {@link ConsentRecord} ledger, plus the purpose-gate signal.
 * Ships the REAL reusable code for the consent CHECK that
 * {@code specs/consent-management-l0.yaml} (CONSENT-PURPOSE-001 "purpose-gated
 * operations MUST check the specific purpose grant", CONSENT-RECORD-001 "absence
 * of a record MUST be treated as no-consent") describes but no domain implemented:
 * all three IDW4 personas hand-rolled "is consent active for this purpose right
 * now" — and each derived it slightly differently (one forgot that a WITHDRAW row
 * supersedes an earlier GRANT; one treated absence-of-record as a hard error
 * instead of no-consent). This gate centralises the derivation so a fork-receiver
 * computes it once, correctly.
 *
 * <h2>activeConsent — the latest-action rule</h2>
 * {@link #activeConsent(String, String, List)} computes current consent for a
 * {@code (subjectId, purpose)} pair from the ledger: consent is active iff the
 * LATEST row for that pair (by {@link ConsentRecord#recordedAt()}) is a
 * {@link ConsentRecord.Action#GRANT}. A later {@link ConsentRecord.Action#WITHDRAW}
 * row supersedes an earlier GRANT (CONSENT-WITHDRAW-001 "takes effect immediately"),
 * and NO row at all is no-consent (CONSENT-RECORD-001 "absence ... treated as
 * no-consent"). Rows for other subjects / other purposes are ignored. This is a
 * pure derivation over an append-only ledger — never a read of a mutable column —
 * which is exactly how the append-only ledger and "current consent is a query"
 * resolve the CONSENT-RECORD-001 contradiction.
 *
 * <h2>requireConsent — the purpose gate</h2>
 * {@link #requireConsent(String, String, List)} returns silently when consent is
 * active, otherwise raises {@link ConsentRequiredException}, which
 * {@link GlobalProblemDetailAdvice} maps to {@code 403 application/problem+json}.
 * Call it at the top of any purpose-gated operation (marketing send, analytics
 * pixel, third-party share) so the gate is a one-liner, not a re-derived check:
 * <pre>{@code
 * // Before sending marketing email, gate on the subject's marketing grant.
 * ConsentGate.requireConsent(subjectId, "marketing_email", ledger.findBySubject(subjectId));
 * }</pre>
 *
 * <h2>Framework-light</h2>
 * The decision logic operates on a plain {@code List<ConsentRecord>} and has no
 * Spring or JPA dependency, so it is trivially unit-testable: build rows with
 * {@link ConsentRecord#grant}/{@link ConsentRecord#withdraw}/{@link ConsentRecord#of}
 * and assert {@link #activeConsent}. All methods are pure / side-effect-free except
 * the deliberate {@code throw} on {@link #requireConsent}.
 */
public final class ConsentGate {

    private ConsentGate() {
    }

    /**
     * Whether consent is currently active for {@code (subjectId, purpose)} given the
     * ledger: true iff the LATEST {@link ConsentRecord} for that pair is a
     * {@link ConsentRecord.Action#GRANT}. A later {@link ConsentRecord.Action#WITHDRAW}
     * supersedes an earlier GRANT; no matching row is no-consent (returns false).
     *
     * <p>Ties on {@link ConsentRecord#recordedAt()} (two rows at the same instant)
     * are broken by id descending so a later-appended row wins, matching the
     * append-only insert order; when ids are also equal/null the WITHDRAW is treated
     * as authoritative (fail-closed — never report consent the trail does not clearly
     * support).
     *
     * @param subjectId the data subject id to evaluate; null/blank → false
     * @param purpose   the declared purpose to evaluate; null/blank → false
     * @param ledger    the consent ledger to derive from; null/empty → false
     * @return true iff current consent for the pair is GRANT
     */
    public static boolean activeConsent(String subjectId, String purpose, List<ConsentRecord> ledger) {
        if (subjectId == null || subjectId.isBlank()
                || purpose == null || purpose.isBlank()
                || ledger == null || ledger.isEmpty()) {
            return false;
        }
        ConsentRecord latest = null;
        for (ConsentRecord r : ledger) {
            if (r == null
                    || !subjectId.equals(r.subjectId())
                    || !purpose.equals(r.purpose())) {
                continue;
            }
            if (latest == null || isLater(r, latest)) {
                latest = r;
            }
        }
        return latest != null && latest.action() == ConsentRecord.Action.GRANT;
    }

    /**
     * Gate a purpose-scoped operation on current consent. Returns silently when
     * {@link #activeConsent(String, String, List)} is true; otherwise raises
     * {@link ConsentRequiredException} (HTTP 403). Call BEFORE performing the
     * purpose-gated processing (marketing send, analytics, third-party share).
     *
     * @throws ConsentRequiredException when consent for the pair is not active
     */
    public static void requireConsent(String subjectId, String purpose, List<ConsentRecord> ledger) {
        if (!activeConsent(subjectId, purpose, ledger)) {
            throw new ConsentRequiredException(purpose);
        }
    }

    /**
     * Order two rows for the SAME (subject, purpose): later {@code recordedAt} wins;
     * on an exact-instant tie a higher id wins (later-appended row); when both are
     * equal/unavailable a WITHDRAW is treated as later than a GRANT (fail-closed).
     */
    private static boolean isLater(ConsentRecord candidate, ConsentRecord current) {
        int byTime = Comparator
                .nullsFirst(Comparator.<java.time.Instant>naturalOrder())
                .compare(candidate.recordedAt(), current.recordedAt());
        if (byTime != 0) {
            return byTime > 0;
        }
        int byId = Comparator
                .nullsFirst(Comparator.<Long>naturalOrder())
                .compare(candidate.id(), current.id());
        if (byId != 0) {
            return byId > 0;
        }
        // Same instant + same/no id: prefer WITHDRAW (fail-closed — never report
        // consent the trail does not unambiguously support).
        return candidate.action() == ConsentRecord.Action.WITHDRAW
                && current.action() == ConsentRecord.Action.GRANT;
    }

    /**
     * Cross-cutting 403 signal raised by {@link ConsentGate#requireConsent} when a
     * purpose-gated operation is attempted without an active consent grant. Anchors
     * {@code specs/consent-management-l0.yaml#CONSENT-PURPOSE-001}
     * ("purpose-gated operations MUST check the specific purpose grant").
     *
     * <p>{@link GlobalProblemDetailAdvice} maps this to {@code 403
     * application/problem+json} (code {@code CONSENT_REQUIRED}). The
     * {@link ResponseStatus @ResponseStatus(FORBIDDEN)} annotation is the
     * belt-and-braces default for any path that bypasses the advice; the explicit
     * handler in the advice is what makes it a direct 403 rather than a
     * {@code /error} re-dispatch (same trap documented on
     * {@link ResourceNotFoundException}).
     */
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static final class ConsentRequiredException extends RuntimeException {

        /** Stable {@code code} a client branches on (carried into the problem+json body). */
        public static final String CODE = "CONSENT_REQUIRED";

        private final transient String purpose;

        public ConsentRequiredException(String purpose) {
            super("Consent required for purpose: " + Objects.toString(purpose, "(none)"));
            this.purpose = purpose;
        }

        /** The purpose whose grant was absent (for a problem+json extension member / log). */
        public String purpose() {
            return purpose;
        }
    }
}

package com.ax.template.authblueprint.common;

import java.util.function.Supplier;

/**
 * Cross-cutting emergency-override ("break-glass") primitive — ships the REAL reusable
 * code for the audited emergency-access pattern that IDW4 (hospital appointment +
 * EMR-lite dogfood, 2026-05-30) found all three personas hand-rolled identically
 * (rule of three).
 *
 * <h2>The gap IDW4 closed</h2>
 * {@link ParticipantScope} is the normal-path control: a provider reads a patient's PHI
 * because a care RELATIONSHIP exists. But regulated domains also need the EXCEPTION
 * path — a clinician with NO relationship to the patient must be able to read PHI in a
 * genuine emergency (the unconscious patient in the ER whose usual physician is not on
 * shift). HIPAA permits this "emergency access procedure", but only when it is
 * <em>recorded</em>: the access is always audited, and typically alerted to a privacy
 * officer for after-the-fact review, precisely because it bypasses the normal
 * relationship gate. All three IDW4 personas re-implemented the same skeleton by hand —
 * "require a reason, write a loud audit record, then run the privileged read" — and the
 * pieces that drifted were the safety-critical ones: one persona forgot to require a
 * reason (so the override left no justification), another logged the override at the
 * same low visibility as a normal read (so it would never surface in review). This
 * primitive centralises the invariant so a fork-receiver cannot get those wrong.
 *
 * <h2>The two non-negotiable obligations this primitive enforces</h2>
 * <ol>
 *   <li><b>A reason is REQUIRED.</b> {@link #invoke} rejects a null/blank reason with
 *       {@link IllegalArgumentException} BEFORE the privileged action runs — a
 *       break-glass access with no recorded justification is not a break-glass access,
 *       it is an unaudited bypass. The action is never executed if the reason is
 *       missing.</li>
 *   <li><b>The access is ALWAYS audited.</b> {@link #invoke} emits a high-visibility
 *       audit signal through the domain-supplied {@link AuditSink} BEFORE returning the
 *       action's result, so the record exists even if the privileged action subsequently
 *       throws. Break-glass access is always audited and typically alerted — wiring the
 *       alert (paging a privacy officer, raising a SIEM event) is a fork-receiver concern
 *       layered on the same sink.</li>
 * </ol>
 *
 * <h2>Why an {@link AuditSink} interface and not a hard AuditLogService dependency</h2>
 * {@code common/} is the leaf package every domain depends ON; it must not depend on a
 * domain package, or the layering guard's no-cycle rule breaks
 * ({@code auditlog} already depends on {@code common}). So this primitive declares a
 * tiny {@link AuditSink} functional interface and the DOMAIN wires
 * {@code com.ax.template.authblueprint.auditlog.AuditLogService#record} to it with a
 * one-line adapter (build an {@code AuditLog} with a {@code BREAK_GLASS} action +
 * {@code SUCCESS}/failure outcome and call {@code record}). The primitive stays
 * framework-light and cycle-free; the domain keeps ownership of the audit schema.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Domain wires the sink once (adapting AuditLogService.record):
 * AuditSink sink = (reason, caller) -> auditLogService.record(
 *     AuditLog.builder()
 *         .actorUserId(caller)
 *         .action(BreakGlass.AUDIT_ACTION)          // "BREAK_GLASS"
 *         .resourceType("Encounter").resourceId(id.toString())
 *         .outcome(AuditOutcome.SUCCESS)
 *         .metadataJson("{\"reason\":\"" + AuditPiiHelper.sanitizeReason(reason) + "\"}")
 *         .build());
 *
 * Encounter e = BreakGlass.invoke(reason, caller.callerId(), sink,
 *     () -> encounterRepository.findById(id).orElseThrow(ResourceNotFoundException::new));
 * }</pre>
 *
 * <p>All members are static and the type is non-instantiable — this is a pure helper,
 * like {@link AuditPiiHelper}.
 */
public final class BreakGlass {

    private BreakGlass() {}

    /**
     * The canonical audit action string a domain SHOULD record for a break-glass access,
     * so emergency overrides are queryable as one class across the audit log (and an
     * alert/review rule can key on a single, stable value). Mirrors the
     * named-constant-not-magic-string discipline used elsewhere in {@code common/}.
     */
    public static final String AUDIT_ACTION = "BREAK_GLASS";

    /**
     * The domain-supplied audit sink for a break-glass access. The primitive depends only
     * on this interface, never on a concrete audit service, so {@code common/} introduces
     * no dependency on a domain package (see the class javadoc rationale). A typical
     * implementation adapts {@code AuditLogService.record} with the {@link #AUDIT_ACTION}
     * action and a high-visibility outcome, and MAY additionally raise an alert.
     */
    @FunctionalInterface
    public interface AuditSink {
        /**
         * Record a break-glass access.
         *
         * @param reason the (already-validated non-blank) justification for the override;
         *               the implementation SHOULD scrub it via
         *               {@link AuditPiiHelper#sanitizeReason(String)} before persisting,
         *               since a free-text emergency reason can contain PII
         * @param caller the id of the actor performing the override (typically the
         *               authenticated caller / clinician id)
         */
        void recordBreakGlass(String reason, String caller);
    }

    /**
     * Perform a break-glass (emergency-override) access: require a non-blank reason,
     * record a high-visibility audit signal through {@code sink}, then run and return the
     * privileged {@code action}'s result.
     *
     * <p>Ordering is deliberate and load-bearing:
     * <ol>
     *   <li>validate the reason first — a missing reason aborts with no audit noise and
     *       WITHOUT running the privileged action;</li>
     *   <li>emit the audit signal next — so the override is recorded even if the
     *       privileged action subsequently throws;</li>
     *   <li>run the action last and return its result.</li>
     * </ol>
     *
     * @param reason the justification for the emergency override; MUST be non-null and
     *               non-blank
     * @param caller the id of the actor performing the override (for the audit record);
     *               MUST be non-null and non-blank
     * @param sink   the domain-wired audit sink; MUST be non-null
     * @param action the privileged action to run under break-glass (typically the PHI
     *               read that the normal {@link ParticipantScope} gate would have
     *               denied); MUST be non-null
     * @param <T>    the action's result type
     * @return the value produced by {@code action}
     * @throws IllegalArgumentException when the reason is blank, the caller is blank, or
     *                                  any argument is null — the action is NOT run in
     *                                  that case (no unaudited bypass)
     */
    public static <T> T invoke(String reason, String caller, AuditSink sink, Supplier<T> action) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("break-glass access requires a non-blank reason");
        }
        if (caller == null || caller.isBlank()) {
            throw new IllegalArgumentException("break-glass access requires a non-blank caller id");
        }
        if (sink == null) {
            throw new IllegalArgumentException("break-glass access requires an audit sink");
        }
        if (action == null) {
            throw new IllegalArgumentException("break-glass access requires an action");
        }
        sink.recordBreakGlass(reason, caller);
        return action.get();
    }
}

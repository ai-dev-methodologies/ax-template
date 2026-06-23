package com.ax.template.authblueprint.statemutation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * state-conditional-mutability-l0 sole orchestrator. The mutation authority is a DECLARED
 * per-(state, field) table ({@link StateFieldPolicy}) the service looks up — never an if-scatter
 * (STATEMUTATION-AUTHORITY/DECLARED-001). Every edit takes the form's PESSIMISTIC_WRITE row lock
 * and re-checks the authority against the state that holds UNDER the lock, so a concurrent forward
 * transition that froze the field makes the racing edit a deterministic 409 rather than a
 * stale-state write (STATEMUTATION-TOCTOU-001, CWE-367). Status moves ONLY through
 * {@link GovernedFormStateMachine} (the sole status mutator), which tightens forward monotonically
 * and records a widening (re-open) as an immutable FormTransition. The DRAFT mutable-set, the state
 * graph, and the freeze points are a fork-receiver swap behind the governance contract.
 * FormTransition rows are members: {@link GovernedFormStateMachine} writes via MemberWriter,
 * root-JPQL reads here.
 */
@Service
public class StateMutationService {

    private final GovernedFormRepository forms;
    private final GovernedFormStateMachine stateMachine;
    private final StateMutationMetrics metrics;
    private final Clock clock;

    public StateMutationService(GovernedFormRepository forms, GovernedFormStateMachine stateMachine,
                                StateMutationMetrics metrics, Clock clock) {
        this.forms = forms;
        this.stateMachine = stateMachine;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public GovernedForm open(String owner, String title, String body) {
        GovernedForm f = new GovernedForm(UUID.randomUUID(), owner, title, body, Instant.now(clock));
        GovernedForm saved = forms.save(f);
        metrics.record("open", "ok");
        return saved;
    }

    /**
     * STATEMUTATION-AUTHORITY/TOCTOU-001 — edit one field, with the authority re-checked against the
     * state that holds UNDER the form's PESSIMISTIC_WRITE lock. If a concurrent forward transition
     * advanced the form past the state in which the field was mutable (a SUBMIT that froze the title
     * between the caller's read and this edit), the racing edit is rejected 409 FIELD_LOCKED_IN_STATE
     * against the CURRENT (advanced) state — never written against the stale state the caller observed
     * (CWE-367). The set named in the rejection comes from the SAME declared table the GET surfaces.
     */
    @Transactional
    public GovernedForm editField(UUID formId, FormField field, String value) {
        GovernedForm f = forms.findByIdForUpdate(formId).orElseThrow(StateMutationException::notFound);
        if (!StateFieldPolicy.isMutable(f.getState(), field)) {
            metrics.record("edit", "field_locked");
            throw StateMutationException.fieldLocked(field, f.getState());   // 409 — names field + state
        }
        f.applyEdit(field, value, Instant.now(clock));
        metrics.record("edit", "edited");
        return f;
    }

    /**
     * STATEMUTATION-MONOTONE-001 — move the form to {@code to} through the state machine (the sole
     * status mutator) under the form's row lock. A FORWARD edge tightens the mutable-set; a REOPEN
     * (widening) requires a recorded reason. An illegal edge is 409; a blank re-open reason is 422.
     * The seq is computed under the lock so the appended transition trail is gap-free and ordered.
     */
    @Transactional
    public GovernedForm transition(UUID formId, FormState to, String reason, String actor) {
        GovernedForm f = forms.findByIdForUpdate(formId).orElseThrow(StateMutationException::notFound);
        long seq = forms.countTransitions(formId);
        try {
            stateMachine.transition(f, to, reason, actor, seq);
        } catch (StateMutationException ex) {
            metrics.record("transition", ex.code().equals("REOPEN_REASON_REQUIRED")
                ? "reopen_no_reason" : "illegal_transition");
            throw ex;
        }
        metrics.record("transition", "transitioned");
        return f;
    }

    @Transactional(readOnly = true)
    public GovernedForm get(UUID formId) {
        return forms.findById(formId).orElseThrow(StateMutationException::notFound);
    }

    @Transactional(readOnly = true)
    public List<FormTransition> transitions(UUID formId) {
        get(formId);                                             // 404 before an empty list
        return forms.findTransitions(formId);
    }
}

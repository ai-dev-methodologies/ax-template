package com.ax.template.authblueprint.statemutation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * state-conditional-mutability-l0 DECLARED per-(state, field) mutation-authority table
 * (STATEMUTATION-AUTHORITY-001 / STATEMUTATION-DECLARED-001). This is the SINGLE place the
 * authority lives: the form's GET surfaces {@link #mutableFields(FormState)} and the edit path
 * enforces the same set — they cannot diverge into an if-scatter that drifts.
 *
 * <p>The table tightens MONOTONICALLY along the forward lifecycle (STATEMUTATION-MONOTONE-001):
 * {@code DRAFT ⊇ SUBMITTED ⊇ APPROVED ⊇ LOCKED}. {@link #isMonotoneForward(FormState, FormState)}
 * lets the state machine assert a forward edge never WIDENS the set; a widening is only legitimate
 * through an explicit recorded re-open. The sets are immutable views so no caller can mutate the
 * declared policy at runtime.
 */
public final class StateFieldPolicy {

    private static final Map<FormState, Set<FormField>> MUTABLE;
    static {
        Map<FormState, Set<FormField>> m = new EnumMap<>(FormState.class);
        m.put(FormState.DRAFT,
            Collections.unmodifiableSet(EnumSet.of(FormField.TITLE, FormField.BODY, FormField.REVIEWER_NOTE)));
        m.put(FormState.SUBMITTED,
            Collections.unmodifiableSet(EnumSet.of(FormField.REVIEWER_NOTE)));   // title/body frozen on submit
        m.put(FormState.APPROVED, Collections.unmodifiableSet(EnumSet.noneOf(FormField.class)));   // read-only
        m.put(FormState.LOCKED, Collections.unmodifiableSet(EnumSet.noneOf(FormField.class)));     // terminal, read-only
        MUTABLE = Collections.unmodifiableMap(m);
    }

    private StateFieldPolicy() {}

    /** The DECLARED mutable-field-set for {@code state} — an immutable view; never null. */
    public static Set<FormField> mutableFields(FormState state) {
        return MUTABLE.getOrDefault(state, Collections.unmodifiableSet(EnumSet.noneOf(FormField.class)));
    }

    /** STATEMUTATION-AUTHORITY-001 — is {@code field} mutable in {@code state}? */
    public static boolean isMutable(FormState state, FormField field) {
        return mutableFields(state).contains(field);
    }

    /** STATEMUTATION-MONOTONE-001 — a forward edge must SHRINK (or keep) the mutable-set, never widen.
     *  Returns true iff {@code to}'s mutable-set is a subset of {@code from}'s (the forward direction). */
    public static boolean isMonotoneForward(FormState from, FormState to) {
        return mutableFields(from).containsAll(mutableFields(to));
    }
}

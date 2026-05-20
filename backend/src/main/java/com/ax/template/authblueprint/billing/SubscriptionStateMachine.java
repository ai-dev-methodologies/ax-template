package com.ax.template.authblueprint.billing;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * BILLING-STATE-001/002 — sole legal mutator of {@link Subscription#setStatus}.
 * <p>
 * Transitions (mirror of blueprints/billing-manifest.yaml#state_machine):
 * <pre>
 *   TRIAL    → ACTIVE     (TRIAL_END)
 *   TRIAL    → CANCELLED  (SUBSCRIPTION_CANCELLED)
 *   ACTIVE   → PAST_DUE   (PAYMENT_FAILED)
 *   ACTIVE   → ACTIVE     (SUBSCRIPTION_RENEWED)         // periodic renewal
 *   ACTIVE   → CANCELLED  (SUBSCRIPTION_CANCELLED)
 *   PAST_DUE → ACTIVE     (PAYMENT_SUCCEEDED)
 *   PAST_DUE → CANCELLED  (SUBSCRIPTION_CANCELLED)
 * </pre>
 * Any transition not listed throws {@link IllegalStateException}.
 */
@Component
public class SubscriptionStateMachine {

    private static final Map<SubscriptionStatus, Set<SubscriptionStatus>> LEGAL = new EnumMap<>(SubscriptionStatus.class);

    static {
        LEGAL.put(SubscriptionStatus.TRIAL, Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED));
        LEGAL.put(SubscriptionStatus.ACTIVE, Set.of(SubscriptionStatus.PAST_DUE, SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED));
        LEGAL.put(SubscriptionStatus.PAST_DUE, Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED));
        LEGAL.put(SubscriptionStatus.CANCELLED, Set.of()); // terminal
    }

    /**
     * Mutates {@code sub} into {@code next}. Returns {@code true} if the
     * transition was applied; throws {@link IllegalStateException} if the
     * transition is illegal.
     */
    public boolean transition(Subscription sub, SubscriptionStatus next) {
        SubscriptionStatus current = sub.getStatus();
        Set<SubscriptionStatus> allowed = LEGAL.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new IllegalStateException(
                "illegal subscription transition: " + current + " -> " + next);
        }
        sub.setStatus(next);
        return true;
    }

    /** Convenience: legal? without mutation. */
    public boolean canTransition(SubscriptionStatus from, SubscriptionStatus to) {
        return LEGAL.getOrDefault(from, Set.of()).contains(to);
    }
}

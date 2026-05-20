package com.ax.template.authblueprint.billing;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BILLING-STATE-001/002 unit checks. Pure JVM, no Spring context.
 */
@Tag("BILLING")
@Tag("BILLING-STATE-002")
class BillingStateMachineTest {

    private final SubscriptionStateMachine sm = new SubscriptionStateMachine();

    private static Subscription trialSub() {
        Plan plan = Plan.create("Basic", 9900L, "KRW", BillingCycle.MONTHLY);
        return Subscription.createTrial("user-1", plan, "stripe");
    }

    @Test
    void trialToActiveIsLegal() {
        Subscription sub = trialSub();
        sm.transition(sub, SubscriptionStatus.ACTIVE);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void activeToPastDueIsLegal() {
        Subscription sub = trialSub();
        sm.transition(sub, SubscriptionStatus.ACTIVE);
        sm.transition(sub, SubscriptionStatus.PAST_DUE);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    @Test
    void pastDueBackToActiveIsLegal() {
        Subscription sub = trialSub();
        sm.transition(sub, SubscriptionStatus.ACTIVE);
        sm.transition(sub, SubscriptionStatus.PAST_DUE);
        sm.transition(sub, SubscriptionStatus.ACTIVE);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void cancelledIsTerminal() {
        Subscription sub = trialSub();
        sm.transition(sub, SubscriptionStatus.CANCELLED);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);

        // No transition out of CANCELLED — any attempt throws (BILLING-STATE-001).
        assertThatThrownBy(() -> sm.transition(sub, SubscriptionStatus.ACTIVE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("illegal subscription transition");
    }

    @Test
    void trialToPastDueIsIllegal() {
        Subscription sub = trialSub();
        assertThatThrownBy(() -> sm.transition(sub, SubscriptionStatus.PAST_DUE))
            .isInstanceOf(IllegalStateException.class);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.TRIAL); // unchanged
    }

    @Test
    void canTransitionMirrorsLegalSet() {
        assertThat(sm.canTransition(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE)).isTrue();
        assertThat(sm.canTransition(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE)).isTrue();
        assertThat(sm.canTransition(SubscriptionStatus.CANCELLED, SubscriptionStatus.ACTIVE)).isFalse();
        assertThat(sm.canTransition(SubscriptionStatus.TRIAL, SubscriptionStatus.PAST_DUE)).isFalse();
    }
}

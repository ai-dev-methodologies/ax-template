package ax.template.saas;

// CORRECT: same multi-L4 composition, but the domain README declares
// applied_recipe: saas-subscription — recipe_governance_guard.sh PASS.
// rule: prefer-recipe-composition-over-l4-cross-import

import ax.template.billing.SubscriptionService;
import ax.template.featureflags.FeatureFlagEvaluator;
import ax.template.notification.NotificationService;

import org.springframework.stereotype.Service;

/**
 * PASSING FIXTURE — rule: prefer-recipe-composition-over-l4-cross-import
 *
 * Same cross-L4 wiring, but README.md declares:
 *   applied_recipe: saas-subscription
 * Guard reads README → finds applied_recipe → PASS.
 */
@Service
class SaasSubscriptionOrchestrator {

    private final SubscriptionService subscriptions;
    private final FeatureFlagEvaluator flags;
    private final NotificationService notifications;

    SaasSubscriptionOrchestrator(
            SubscriptionService subscriptions,
            FeatureFlagEvaluator flags,
            NotificationService notifications) {
        this.subscriptions = subscriptions;
        this.flags = flags;
        this.notifications = notifications;
    }

    void onPlanUpgrade(String tenantId, String newPlan) {
        subscriptions.changePlan(tenantId, newPlan);
        flags.enableForTenant(tenantId, "premium_features");
        notifications.sendUpgradeConfirmation(tenantId, newPlan);
    }
}

package ax.template.saas;

// VIOLATION: saas domain imports from 3 sibling L4 domains ad-hoc
// without applied_recipe: in the domain README.
// rule: prefer-recipe-composition-over-l4-cross-import
// Guard must catch: multi-L4 cross-imports without recipe declaration.

import ax.template.billing.SubscriptionService;        // ← L4/billing cross-import
import ax.template.featureflags.FeatureFlagEvaluator;  // ← L4/feature-flags cross-import
import ax.template.notification.NotificationService;   // ← L4/notification cross-import

import org.springframework.stereotype.Service;

/**
 * FAILING FIXTURE — rule: prefer-recipe-composition-over-l4-cross-import
 *
 * Violation: SaasSubscriptionOrchestrator directly imports from 3 L4 domain packages
 * (billing, feature-flags, notification) without the owning domain README declaring
 * applied_recipe: saas-subscription.
 *
 * This duplicates the saas-subscription RECIPE.md composition contract out-of-band.
 * ArchUnit: noClasses in ..saas.. should import from 3+ L4 packages without recipe metadata.
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

    /**
     * VIOLATION: manually implements saas-subscription composition without
     * declaring the pattern in README — drift from RECIPE.md goes undetected.
     */
    void onPlanUpgrade(String tenantId, String newPlan) {
        subscriptions.changePlan(tenantId, newPlan);
        flags.enableForTenant(tenantId, "premium_features");
        notifications.sendUpgradeConfirmation(tenantId, newPlan);
    }
}

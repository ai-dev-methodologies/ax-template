/**
 * FIXTURE: prefer-recipe-composition-over-l4-cross-import/fail_ad_hoc_cross_import
 * Demonstrates WRONG pattern: L4/saas page importing from 3 sibling L4 domains
 * without applied_recipe: in templates/L4/saas/README.md.
 * Guard must catch: cross-L4 composition without recipe declaration.
 */
"use client";

// VIOLATION: 3 sibling L4 domain imports without applied_recipe: declaration
// This duplicates saas-subscription RECIPE.md composition contract out-of-band.
import { useSubscription } from "templates/L4/billing/hooks/useSubscription";           // ← L4/billing
import { useFeatureFlags } from "templates/L4/feature-flags/hooks/useFeatureFlags";     // ← L4/feature-flags
import { useNotificationBanner } from "templates/L4/notification/hooks/useNotificationBanner"; // ← L4/notification

export default function SaasDashboardPage() {
  // VIOLATION: wires billing + feature-flags + notification without recipe metadata
  const { plan, usagePercent } = useSubscription();
  const { isPremium } = useFeatureFlags("premium_features");
  const banner = useNotificationBanner();

  return (
    <div>
      <h1>Dashboard — {plan}</h1>
      <p>Usage: {usagePercent}%</p>
      {isPremium && <span>Premium</span>}
      {banner && <p>{banner}</p>}
    </div>
  );
}

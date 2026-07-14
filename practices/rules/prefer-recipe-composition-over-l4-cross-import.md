---
title: "When a business domain matches a Business Pattern Recipe, cross-L4 wiring must follow the Recipe composition contract; ad-hoc multi-L4 cross-imports without applied_recipe declaration are prohibited"
rule_id: prefer-recipe-composition-over-l4-cross-import
impact: HIGH
impactDescription: "Ad-hoc cross-L4 imports that duplicate a Recipe's composition contract create undeclared coupling between domains, make the recipe audit trail invisible to tooling, and produce two incompatible wiring paths for the same business pattern"
tags:
  - architecture
  - recipe-composition
  - l4-layer
  - domain-isolation
  - composition-kit
provenance_class: internal_design
protects_template_id: recipes/*/RECIPE.md
failing_fixture_path: practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/fail_ad_hoc_cross_import/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ARCH-001"
verification:
  type: review
  notes: |
    ArchUnit: detect Spring services that import from 2+ L4 domain packages
    (ax.template.billing + ax.template.notification + ax.template.featureflags, etc.)
    when the owning L4 domain README lacks applied_recipe: field.
    Acceptable: single-hop cross-L4 for shared utilities.
    Violation: multi-domain composition without recipe declaration.
evidence:
  - source_type: external
    anchors: generic_principle_only
    citation: "Martin Fowler — Patterns of Enterprise Application Architecture: composition patterns prevent ad-hoc coupling by making dependencies explicit through a shared composition contract"
    url: "https://martinfowler.com/eaaCatalog/"
    quoted_at: "2026-05-18"
  - source_type: external
    anchors: generic_principle_only
    citation: "Spring Modulith reference — modules communicate via published events or explicit API types; direct package imports between modules create structural coupling that Spring Modulith enforces at test time"
    url: "https://docs.spring.io/spring-modulith/reference/fundamentals.html"
    quoted_at: "2026-05-18"
  - source_type: external
    anchors: generic_principle_only
    citation: "토스 기술 블로그 — 도메인 모듈 설계: 도메인 간 직접 의존 대신 이벤트 또는 명시적 조합 계약을 통해 결합도를 낮춥니다"
    url: "https://toss.tech/article/slash21-backend"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## prefer-recipe-composition-over-l4-cross-import (Java)

**Impact: HIGH — When a business domain matches a shipped Recipe Pattern (saas-subscription, e-commerce, crm), the cross-L4 wiring must declare its recipe via `applied_recipe:`. Ad-hoc multi-L4 imports without this declaration create invisible coupling and duplicate the recipe composition out-of-band.**

The ax-template composition kit ships Business Pattern Recipes that define *how* multiple L4 domains compose into a coherent business feature. When a Spring service ad-hoc imports from billing, notification, and feature-flags all at once — without `applied_recipe:` in the domain README — it implements a recipe-equivalent pattern off-catalog. This defeats the guard chain and breaks the composition audit trail.

**Incorrect — multi-L4 composition without recipe declaration:**

```java
// VIOLATION: SaasSubscriptionOrchestrator wires billing + feature-flags + notification
// without applied_recipe: in the L4 domain README → ad-hoc recipe duplicate
package ax.template.saas;

import ax.template.billing.SubscriptionService;        // ← L4/billing cross-import
import ax.template.featureflags.FeatureFlagEvaluator;  // ← L4/feature-flags cross-import
import ax.template.notification.NotificationService;   // ← L4/notification cross-import

import org.springframework.stereotype.Service;

/**
 * WRONG: Manually implements saas-subscription composition without
 * declaring applied_recipe: saas-subscription in the domain README.
 * ArchUnit flags: 3 L4-package cross-imports without recipe metadata.
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
        // ad-hoc wiring of billing → feature-flags → notification
        // duplicates saas-subscription RECIPE.md composition contract
        subscriptions.changePlan(tenantId, newPlan);
        flags.enableForTenant(tenantId, "premium_features");
        notifications.sendUpgradeConfirmation(tenantId, newPlan);
    }
}
```

**Correct — domain README declares applied_recipe; composition follows the contract:**

```java
// CORRECT: The L4 domain README declares:
//   applied_recipe: saas-subscription
// The orchestrator still wires billing + feature-flags + notification but
// the recipe metadata makes the composition explicit and guard-visible.

package ax.template.saas;

import ax.template.billing.SubscriptionService;
import ax.template.featureflags.FeatureFlagEvaluator;
import ax.template.notification.NotificationService;

import org.springframework.stereotype.Service;

/**
 * CORRECT: domain README carries applied_recipe: saas-subscription.
 * recipe_governance_guard.sh validates this wiring matches RECIPE.md.
 */
@Service
class SaasSubscriptionOrchestrator {
    // same wiring — the declaration makes it compliant
    void onPlanUpgrade(String tenantId, String newPlan) {
        subscriptions.changePlan(tenantId, newPlan);
        flags.enableForTenant(tenantId, "premium_features");
        notifications.sendUpgradeConfirmation(tenantId, newPlan);
    }
}
```

### Detection

ArchUnit: `noClasses().that().resideInAPackage("..saas..")` imports 2+ distinct L4 packages AND corresponding README lacks `applied_recipe:` field.

## Failing fixture

See: `practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/fail_ad_hoc_cross_import/SaasOrchestrator.java` — three L4 cross-imports without recipe declaration. Guard must flag.

See: `practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/pass/SaasOrchestrator.java` — same imports with `applied_recipe: saas-subscription` in companion README.md.

Reference: https://martinfowler.com/eaaCatalog/

# L4 Composition — saas-subscription

> Which L4 domains to enable and how they wire together.

## Domain Wiring

```
auth
 └── issues JWT with { userId, tenantId, role }
      ↓
billing
 └── uses tenantId to scope subscription + plan lookup
      ↓
feature-flags
 └── evaluates flags against { userId, planTier } context
      ↓
notification
 └── receives BillingEvent (trial_ending, invoice_due, payment_failed) → sends email/push
      ↓
audit-log
 └── records every billing state-change (plan_activated, plan_downgraded, subscription_cancelled)
```

## Domain Configuration Notes

### `auth`
- Enable multi-tenant mode: JWT payload must include `tenantId` claim
- RBAC roles: `TENANT_OWNER`, `TENANT_ADMIN`, `TENANT_MEMBER`
- Reference: `templates/L4/auth/`

### `billing`
- Enable Toss Payments recurring billing provider (`BillingProvider.TOSS`)
- Configure plan tiers in `billing-manifest.yaml`: FREE / STARTER / PRO / ENTERPRISE
- Idempotency key required on all billing state transitions per `rule_ref: practices/rules/billing-event-idempotent.md`
- Reference: `templates/L4/billing/`

### `feature-flags`
- Evaluation context includes `planTier` from billing subscription
- Gate enforcement: call `FeatureFlagService.isEnabled(flagName, userId, planTier)` at entry points
- Default deny on unknown flags
- Reference: `templates/L4/feature-flags/`

### `notification`
- Subscribe to `BillingEventPublished` via `@EventListener`
- Notification channels: email (via email-outbox), in-app push
- Templates: `trial_ending_3d`, `invoice_due`, `payment_failed`, `plan_downgraded`
- Reference: `templates/L4/notification/`

### `audit-log`
- Annotate billing service methods with `@Audited(action = "billing.plan.changed")`
- Retention: ≥90 days per `spec_ref: specs/audit-log-l0.yaml`
- Immutable: no UPDATE/DELETE on `audit_logs` table
- Reference: `templates/L4/audit-log/`

## Applied Recipe Annotation

Every L4 domain wired under this recipe **must** declare in its README.md or Spec Trio metadata:
```
applied_recipe: saas-subscription
```
(Enforced by rule `business-domain-must-declare-applied-recipe` — SP37)

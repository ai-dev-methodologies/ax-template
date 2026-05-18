# L2 Block Selection — saas-subscription

> Which existing L2 blocks to use and in what composition order.

## Block Inventory

All blocks listed here already exist at `templates/L2/blocks/`. No new L2 blocks are introduced by this recipe.

| Block | File | Usage in recipe | L3 page |
|---|---|---|---|
| `pricing-table` | `pricing-table.tsx` | Displays available plans with feature comparison | `pricing-page` |
| `plan-comparison` | `plan-comparison.tsx` | Side-by-side plan tier comparison | `pricing-page` |
| `usage-meter` | `usage-meter.tsx` | Shows current usage vs. plan limit | `settings-overview` |
| `invoice-list` | `invoice-list.tsx` | Historical invoice index with download | `settings-overview` |
| `billing-history` | `billing-history.tsx` | Timeline of billing events (charges, refunds) | `settings-overview` |
| `feature-flag-toggle` | `feature-flag-toggle.tsx` | Admin toggle for individual feature flags | `admin-overview-page` |
| `feature-gate` | `feature-gate.tsx` | Wraps any UI section; hides if flag inactive | Any gated section |
| `kpi-card` | `kpi-card.tsx` | MRR, active subscriptions, churn rate KPIs | `admin-overview-page` |

## Composition Order

```
pricing-page
  ├── pricing-table        ← plan selection CTA
  └── plan-comparison      ← feature matrix

settings-overview
  ├── usage-meter          ← current period usage bar
  ├── invoice-list         ← paginated invoice history
  └── billing-history      ← event timeline

admin-overview-page
  ├── kpi-card (MRR)
  ├── kpi-card (active_subscriptions)
  ├── kpi-card (churn_rate)
  ├── feature-flag-toggle  ← admin toggles per flag
  └── feature-gate         ← wraps experimental sections
```

## Notes

- `feature-gate` is a wrapper component — use it at any level to conditionally render UI based on the plan tier.
- `kpi-card` accepts a `metric` prop; instantiate once per KPI metric.
- `usage-meter` reads from the billing domain's `/api/billing/usage` endpoint; no direct DB access from frontend.

# L2 Block Selection — b2b-admin

> Which existing L2 blocks to use and in what composition order.

## Block Inventory

All blocks listed here already exist at `templates/L2/blocks/`. No new L2 blocks are introduced by this recipe.

| Block | File | Usage in recipe | L3 page |
|---|---|---|---|
| `impersonation-banner` | `impersonation-banner.tsx` (SP34) | Top banner shown during impersonation session | global layout |
| `kpi-card` | `kpi-card.tsx` | Tenant count, active users, flag mutation rate | `dashboard-page`, `admin-overview-page` |
| `time-series-chart` | `time-series-chart.tsx` | Tenant activity over time; flag change history trend | `dashboard-page` |
| `data-table` | `data-table.tsx` | Tenant list, user list, audit-log entries | `list-page`, `audit-log-page` |
| `filter-bar` | `filter-bar.tsx` | Tenant / user / date filter inputs | `list-page`, `audit-log-page` |
| `bulk-actions-bar` | `bulk-actions-bar.tsx` | Bulk suspend / export selected tenants/users | `list-page` (admin) |
| `bulk-export` | `bulk-export.tsx` | CSV/XLSX export of tenant or audit data | `list-page`, `audit-log-page` |
| `search-palette` | `search-palette.tsx` | Cross-tenant quick-search (ADMIN) | global nav |
| `feature-flag-toggle` | `feature-flag-toggle.tsx` | Per-tenant flag on/off toggle | `detail-page` (tenant), `settings-overview` |
| `feature-gate` | `feature-gate.tsx` | Hide admin-only sections from MANAGER role | multiple pages |
| `column-picker` | `column-picker.tsx` | Choose visible columns in tenant/user table | `list-page` |
| `column-reorder` | `column-reorder.tsx` | Drag-reorder visible columns | `list-page` |
| `saved-view` | `saved-view.tsx` | Save named table view (column set + filter preset) | `list-page` |
| `saved-filters` | `saved-filters.tsx` | Save named filter combination | `list-page`, `audit-log-page` |

## Composition Order

```
global layout (authenticated)
  └── impersonation-banner   ← shown when JWT contains impersonating claim

admin-overview-page
  ├── kpi-card × 3           ← tenant count / active users / flag change rate
  └── time-series-chart      ← tenant activity sparkline

dashboard-page (per tenant)
  ├── kpi-card × 3           ← tenant-scoped KPIs
  └── time-series-chart      ← tenant activity (scoped)

list-page (tenants — ADMIN)
  ├── search-palette          ← cross-tenant quick-search
  ├── filter-bar              ← status / plan / region filter
  ├── column-picker           ← column visibility
  ├── column-reorder          ← column ordering
  ├── saved-view              ← saved column + filter preset
  └── data-table              ← tenant rows with bulk-actions-bar

list-page (users — ADMIN/MANAGER)
  ├── filter-bar              ← role / status / date filter
  ├── saved-filters           ← named filter presets
  ├── bulk-actions-bar        ← bulk suspend / export
  └── data-table              ← user rows

audit-log-page
  ├── filter-bar              ← actor / action / date filter
  ├── saved-filters           ← named audit query presets
  ├── bulk-export             ← CSV/XLSX audit export
  └── data-table              ← audit-log rows (read-only, immutable)

detail-page (tenant)
  ├── feature-flag-toggle × N ← per-tenant flags (MANAGER can toggle own; ADMIN any)
  └── feature-gate            ← hides destructive ADMIN-only actions from MANAGER

settings-overview
  └── feature-flag-toggle × N ← org-level flags for logged-in tenant
```

## Notes

- `impersonation-banner` reads `impersonating` claim from JWT; shows exit-impersonation button that calls POST /api/auth/impersonate/end.
- `feature-gate` checks `role` claim; does NOT make API calls — client-side gate only. Server enforces separately.
- `search-palette` sends to ADMIN-only cross-tenant search endpoint; for non-ADMIN users, the block is hidden via `feature-gate`.
- `saved-view` / `saved-filters` state stored in `localStorage` + optionally synced to user profile endpoint.

# L2 Block Selection — api-gateway-relay

> Which existing L2 blocks to use and in what composition order.

## Block Inventory

All blocks listed here already exist at `templates/L2/blocks/` (disk-verified
at SP47 prep via `ls templates/L2/blocks/*.tsx` on 2026-05-23). No new L2
blocks are introduced by this recipe.

| Block | File | Usage in recipe | L3 page |
|---|---|---|---|
| `crud-create-form` | `crud-create-form.tsx` | Register a new route (URL + method + target + signing-secret) | `create-page` |
| `crud-edit-form` | `crud-edit-form.tsx` | Edit existing route (toggle active, rotate signing-secret, change rate-limit policy) | `edit-page` |
| `crud-list-adapter` | `crud-list-adapter.tsx` | Paginated route list (filterable by active / circuit-state / target) | `list-page` |
| `data-table` | `data-table.tsx` | Operator route inspection table (route + target + status + last-relay + dead-letter-count columns) | `list-page` (operator) |
| `filter-bar` | `filter-bar.tsx` | Active / circuit-state (CLOSED / HALF_OPEN / OPEN) / target-host filter chips | `list-page` |
| `kpi-card` | `kpi-card.tsx` | Routes-active / 429-rejections-today / dead-letter-size / circuit-open-count | `dashboard-page` |
| `confirm-dialog` | `confirm-dialog.tsx` | Pause / resume / rotate-secret / replay-dead-letter confirmation | `detail-page`, `list-page` |
| `bulk-actions-bar` | `bulk-actions-bar.tsx` | Bulk pause / resume / circuit-reset across selected routes | `list-page` (operator) |

### L1 primitives consumed (informational; not in spec `l2_blocks_used:`)

| L1 Primitive | File | Usage |
|---|---|---|
| `code-block` | `templates/L1/components/code-block.tsx` | Request / response payload preview in route detail-page |
| `relative-time` | `templates/L1/components/relative-time.tsx` | "Last relayed 2m ago" / "Circuit opened 5m ago" timestamps |
| `badge` | `templates/L1/components/badge.tsx` | Route status indicator chips: ACTIVE / PAUSED / CIRCUIT-OPEN / DEAD-LETTER |

L1 primitives are excluded from the recipe spec's `l2_blocks_used:` list because
the `recipe_spec_referential_integrity_guard.sh` resolves `l2_blocks_used:`
entries against `templates/L2/blocks/<name>.tsx` ONLY. L1 primitives are
documented here for AI implementers but not gated by the guard (same pattern as
`booking-recipe-l0.yaml` exclusion of `calendar` / `date-range-picker` /
`relative-time`, `community-recipe-l0.yaml` exclusion of `rich-text-editor` /
`markdown-renderer`, and `internal-it-recipe-l0.yaml` exclusion of
`relative-time` / `priority-badge` / `status-badge`).

## Composition Order

```
list-page (operator route inspection)
  ├── filter-bar             ← active / circuit-state / target-host chips
  ├── data-table             ← rows with route + target + status + last-relay
  │                            + dead-letter-count cols (status uses
  │                            `badge` L1 chip)
  ├── bulk-actions-bar       ← bulk pause / resume / circuit-reset
  └── confirm-dialog         ← per-action confirmation (pause / resume /
                              circuit-reset / rotate-secret)

create-page (register new route)
  └── crud-create-form       ← URL pattern + method + target URI +
                              signing-secret (auto-generated) +
                              rate-limit policy + active toggle +
                              idempotency-key (X-Idempotency-Key on POST)

edit-page (operator — modify route config)
  └── crud-edit-form         ← toggle active / rotate signing-secret /
                              change rate-limit policy; AUDIT-RECORD-002
                              before/after diff captured server-side

detail-page (single route — full state + history)
  ├── confirm-dialog         ← replay-dead-letter / rotate-secret prompts
  └── (informational) `code-block` L1 + `relative-time` L1 for last-relay
                              payload preview + timestamp

dashboard-page (operator landing — gateway-wide health)
  ├── kpi-card × 4           ← routes-active / 429-rejections-today /
                              dead-letter-size / circuit-open-count
  └── crud-list-adapter      ← compact recent-relays feed (top 20)
```

## Notes

- `confirm-dialog` text varies by action (pause / resume / circuit-reset /
  rotate-secret / replay-dead-letter); audit-log row writes regardless of
  dialog branch (INV-005 AUDIT-RECORD-002 immutability).
- Circuit-breaker reconciliation is fully server-side (`scheduled-task`
  `CircuitBreakerReconcileTask`) — the L2 surface only shows the *result*
  (kpi-card `circuit-open-count` + `data-table` status column +
  `badge` chip).
- Dead-letter replay UI is operator-driven via `confirm-dialog` on the
  `detail-page`; the underlying admin endpoint mirrors the webhook L4's
  `POST /webhook-deliveries/{id}/replay` shape (R9 SP45 introduction). The
  replay UI itself is fork-receiver responsibility — `templates/L4/webhook/contracts/webhook-openapi.yaml`
  documents the admin endpoint.
- Idempotency-key on route mutations (`POST /api/admin/routes`) is documented
  in the `crud-create-form` error-binding contract; the operator UI generates a
  stable `X-Idempotency-Key` (uuid) per form submission to avoid double-create
  on accidental retry. INV-005 binds the rule via
  `rule_ref: practices/rules/idempotency-key-on-mutations.md`.
- Rate-limit policy (per-route) is exposed as an editable JSON config field on
  `crud-edit-form` (max-per-window + window-millis + key-strategy). The
  cross-cutting `specs/ratelimit-l0.yaml#RATELIMIT-1/2` spec items govern the
  HTTP 429 + Retry-After semantics; the L2 surface only shows the *policy
  definition*.

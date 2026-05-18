# L2 Block Selection — internal-it

> Which existing L2 blocks to use and in what composition order.

## Block Inventory

All blocks listed here already exist at `templates/L2/blocks/`. No new L2 blocks
are introduced by this recipe.

| Block | File | Usage in recipe | L3 page |
|---|---|---|---|
| `crud-create-form` | `crud-create-form.tsx` | New ticket open form (title + description + priority + assignee) | `create-page` |
| `crud-edit-form` | `crud-edit-form.tsx` | Edit ticket fields (re-assign, change priority, add comment) | `edit-page` |
| `crud-list-adapter` | `crud-list-adapter.tsx` | Paginated ticket list (filterable by state / assignee / priority) | `list-page` |
| `data-table` | `data-table.tsx` | Operator triage queue (state + age + SLA-remaining columns) | `list-page` (operator) |
| `filter-bar` | `filter-bar.tsx` | State / priority / assignee / SLA-status filter chips | `list-page` |
| `kpi-card` | `kpi-card.tsx` | Active tickets / breaching SLA / resolved-this-week / webhook-dead-letter-size | `dashboard-page` |
| `notification-bell` | `notification-bell.tsx` | In-app notification counter for assignee fanout | `dashboard-page` header |
| `notification-list` | `notification-list.tsx` | Assignee notification feed | `dashboard-page`, `detail-page` |
| `confirm-dialog` | `confirm-dialog.tsx` | Resolve / close / escalate confirmation | `detail-page`, `list-page` |

### L1 primitives consumed (informational; not in spec `l2_blocks_used:`)

| L1 Primitive | File | Usage |
|---|---|---|
| `relative-time` | `templates/L1/components/relative-time.tsx` | "Opened 2h ago" / "SLA breach in 30m" timestamps on rows |
| `priority-badge` | (out-of-catalog — combobox reuse with chip render) | Ticket priority visual chip (P1/P2/P3/P4) |
| `status-badge` | (out-of-catalog — combobox reuse with chip render) | Ticket state visual chip (open/in-progress/resolved/closed) |

L1 primitives are excluded from the recipe spec's `l2_blocks_used:` list because
the `recipe_spec_referential_integrity_guard.sh` resolves `l2_blocks_used:`
entries against `templates/L2/blocks/<name>.tsx` only. L1 primitives are
documented here for AI implementers but not gated by the guard (same pattern as
`booking-recipe-l0.yaml` exclusion of `calendar` / `date-range-picker` /
`relative-time` and `community-recipe-l0.yaml` exclusion of `rich-text-editor`
/ `markdown-renderer`).

**Badge UX note:** `priority-badge` and `status-badge` do NOT exist on disk
under either tier — they are deferred to fork-receiver L1 extension OR treated
as `combobox` reuse with chip-style render (same pattern as R8 CMS recipe tag-
input). Catalog discipline preserved by NOT inventing a recipe-level L1
expansion.

## Composition Order

```
list-page (operator triage queue)
  ├── filter-bar             ← state / priority / assignee / SLA-status chips
  ├── data-table             ← rows with state + age + SLA-remaining cols
  └── confirm-dialog         ← per-action confirmation (resolve / escalate)

list-page (requester — own tickets)
  ├── filter-bar             ← state / priority chips only (no assignee filter)
  └── crud-list-adapter      ← paginated rows with state badge + relative-time L1

create-page (open new ticket — requester)
  └── crud-create-form       ← title + description + priority + (auto-assigned)
                                + idempotency-key (X-Idempotency-Key on POST)

edit-page (operator — re-assign / change priority / comment)
  └── crud-edit-form         ← inline state transitions route via gated
                                /api/tickets/{id}/transition endpoint

detail-page (single ticket)
  ├── notification-list      ← ticket-scoped notifications (assigned / status changes / SLA warnings)
  └── confirm-dialog         ← resolve / close / escalate prompt
                              (close requires APPROVER role per auth ASVS V4.1)

dashboard-page (operator landing)
  ├── notification-bell      ← in-header notification counter
  ├── kpi-card × 4           ← active / breaching-SLA / resolved-this-week /
                                webhook-dead-letter-size (counts outbound ITSM
                                relays in FAILED_PERMANENT — alerts on hosed
                                Jira / PagerDuty integrations)
  └── notification-list      ← cross-ticket assignee notifications
```

## Notes

- `confirm-dialog` text varies by action (resolve / close / escalate / re-open);
  audit-log row writes regardless of dialog branch (INTERNAL-IT-INV-001).
- SLA-breach reminder fanout is driven server-side by `scheduled-task`
  (`SlaBreachReminderTask`) — the L2 surface only shows the *result* (kpi-card
  "breaching SLA" count + notification-list entries).
- Outbound webhook relay to external ITSM (INTERNAL-IT-INV-003) is fully
  server-side; the L2 surface only shows operator-visible status via the
  `webhook-dead-letter-size` kpi-card and (via the admin API) the dead-letter
  inspection + replay UI. The replay UI itself is fork-receiver responsibility
  — `templates/L4/webhook/contracts/webhook-openapi.yaml` documents the
  `POST /webhook-deliveries/{id}/replay` admin endpoint.
- Idempotency-key on mutations (`POST /api/tickets`) is documented in
  `crud-create-form` error-binding contract; the requester UI generates a
  stable `X-Idempotency-Key` (uuid) per form submission to avoid double-create
  on accidental retry.

# L2 Block Selection — crm

> Which existing L2 blocks to use and in what composition order.

## Block Inventory

All blocks listed here already exist at `templates/L2/blocks/`. No new L2 blocks are introduced by this recipe.

> **Note:** `pipeline-kanban.tsx` was considered for CRM kanban view but is NOT included — that block does not yet exist. Kanban view is deferred to a future L2 SP if fork-receiver demand arrives.

| Block | File | Usage in recipe | L3 page |
|---|---|---|---|
| `data-table` | `data-table.tsx` | Lead/deal list with sortable columns | `list-page` |
| `activity-feed` | `activity-feed.tsx` | Activity timeline for a contact/deal | `detail-page` |
| `filter-bar` | `filter-bar.tsx` | Stage / owner / date range filter inputs | `list-page` |
| `saved-view` | `saved-view.tsx` | Saved filter sets (e.g., "My Open Deals") | `list-page` |
| `kpi-card` | `kpi-card.tsx` | Pipeline value, win rate, open deals KPIs | `dashboard-page` |
| `event-stream` | `event-stream.tsx` | Real-time deal stage change notifications | `detail-page` (deal) |

## Composition Order

```
dashboard-page
  ├── kpi-card (pipeline_value)
  ├── kpi-card (win_rate)
  └── kpi-card (open_deals_count)

list-page (leads / contacts / deals)
  ├── filter-bar        ← stage / owner / date range
  ├── saved-view        ← "My Open Deals", "This Week's Leads"
  └── data-table        ← sortable rows with stage badges

detail-page (contact / deal)
  ├── activity-feed     ← chronological activity timeline
  └── event-stream      ← live deal stage transition updates

create-page
  └── crud-create-form  ← lead / deal / activity fields
```

## Notes

- `saved-view` persists user-specific filter combinations via the CRUD API (`/api/saved-views`). No separate microservice needed.
- `event-stream` connects to `GET /api/deals/{id}/stream` (SSE from notification L4 domain).
- `activity-feed` is chronological and append-only in the UI — no edit/delete from the feed component. Mutations happen via `detail-page` actions.

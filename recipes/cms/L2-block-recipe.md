# L2 Block Selection — cms

> Which existing L2 blocks to use and in what composition order.

## Block Inventory

All blocks listed here already exist at `templates/L2/blocks/`. No new L2 blocks
are introduced by this recipe.

| Block | File | Usage in recipe | L3 page |
|---|---|---|---|
| `crud-create-form` | `crud-create-form.tsx` | New content authoring form (title + body + slug + locale) | `create-page` |
| `crud-edit-form` | `crud-edit-form.tsx` | Edit content (pre-publish draft revisions) | `edit-page` |
| `crud-list-adapter` | `crud-list-adapter.tsx` | Paginated content list (filterable by state) | `list-page` |
| `data-table` | `data-table.tsx` | Editorial review queue (state-filtered) | `list-page` (editor) |
| `filter-bar` | `filter-bar.tsx` | State / locale / content-type filter chips | `list-page` |
| `kpi-card` | `kpi-card.tsx` | Scheduled-publish pending / archived this week / review queue | `dashboard-page` |
| `notification-list` | `notification-list.tsx` | Editorial workflow notifications | `dashboard-page`, `detail-page` |
| `confirm-dialog` | `confirm-dialog.tsx` | Publish / archive / reject confirmation | `detail-page`, `list-page` |
| `search-input` | `search-input.tsx` | Full-text search across content (optional bind via `search` L4) | `list-page` |

### L1 primitives consumed (informational; not in spec `l2_blocks_used:`)

| L1 Primitive | File | Usage |
|---|---|---|
| `rich-text-editor` | `templates/L1/components/rich-text-editor.tsx` | Content authoring (body composer; server-side validates structure) |
| `markdown-renderer` | `templates/L1/components/markdown-renderer.tsx` | Read-view rendering of published content |
| `relative-time` | `templates/L1/components/relative-time.tsx` | "Last edited 2h ago" timestamps on content rows |

L1 primitives are excluded from the recipe spec's `l2_blocks_used:` list because
the `recipe_spec_referential_integrity_guard.sh` resolves `l2_blocks_used:`
entries against `templates/L2/blocks/<name>.tsx` only. L1 primitives are documented
here for AI implementers but not gated by the guard (same pattern as
`booking-recipe-l0.yaml` exclusion of `calendar` / `date-range-picker` / `relative-time`
and `community-recipe-l0.yaml` exclusion of `rich-text-editor` / `markdown-renderer`).

**Tagging UX note (PRD §4.2 disambiguation):** `tag-input` does NOT exist on
disk under either tier — it has been REMOVED from the inventory. Tagging UX is
deferred to fork-receiver L1 extension OR treated as `combobox` reuse with
chip-style render.

## Composition Order

```
list-page (content)
  ├── search-input           ← full-text search across content (when `search` L4 enabled)
  ├── filter-bar             ← state / locale / content-type chips
  └── crud-list-adapter      ← paginated rows with state badge + last-edit relative-time L1

list-page (editorial review queue — editor)
  ├── filter-bar             ← state / reviewer / submitted-by filter
  ├── data-table             ← review-pending rows with quick-action column
  └── confirm-dialog         ← per-action confirmation (approve / reject)

create-page (content)
  └── crud-create-form       ← title + body (rich-text-editor L1) + slug + locale
                                + scheduled-publish-at (calendar via shared L1)

edit-page (content)
  └── crud-edit-form         ← body revisions; state transitions route via
                                separate gated publish-state endpoint

detail-page (content)
  ├── notification-list      ← content-scoped editorial notifications
  └── confirm-dialog         ← publish / archive / reject prompt

dashboard-page
  ├── kpi-card × 3           ← scheduled-publish pending / archived this week / review queue depth
  └── notification-list      ← cross-content editorial notifications
```

## Notes

- `search-input` is conditional on `search` L4 being enabled (optional bind);
  small-corpus deployments may omit `search` and rely on `filter-bar` only.
- `confirm-dialog` text varies by action (publish / archive / reject); audit-log
  row writes regardless of dialog branch (CMS-INV-001).
- Scheduled-publish + scheduled-archive fanout is driven server-side by the
  scheduled-task cron (`ScheduledPublishTask`, `ContentExpiryTask`) — the L2
  surface only shows the *result* (state badge update + audit row + notification).
- Slug uniqueness (CMS-INV-005) is enforced server-side at controller validation
  per `specs/crud-security.yaml#CRUD-VAL-1`; the L2 form layer surfaces the
  RFC 7807 validation error inline via `crud-create-form` / `crud-edit-form`
  error-binding contract.

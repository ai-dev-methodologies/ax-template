# L2 Block Selection — community

> Which existing L2 blocks to use and in what composition order.

## Block Inventory

All blocks listed here already exist at `templates/L2/blocks/`. No new L2 blocks
are introduced by this recipe.

| Block | File | Usage in recipe | L3 page |
|---|---|---|---|
| `crud-create-form` | `crud-create-form.tsx` | New post / new comment form | `create-page` |
| `crud-edit-form` | `crud-edit-form.tsx` | Edit post / edit comment (pre-moderation) | `edit-page` |
| `crud-list-adapter` | `crud-list-adapter.tsx` | Paginated thread list | `list-page` |
| `data-table` | `data-table.tsx` | Moderator queue with filter chips | `list-page` (admin) |
| `filter-bar` | `filter-bar.tsx` | Status / tag / author filter for thread list | `list-page` |
| `search-input` | `search-input.tsx` | Full-text search across posts + comments | `list-page` |
| `notification-list` | `notification-list.tsx` | Reply notifications panel | `dashboard-page`, `detail-page` |
| `notification-bell` | `notification-bell.tsx` | Unread reply badge in header | (global app shell) |
| `confirm-dialog` | `confirm-dialog.tsx` | Moderation action confirmation (hide / lock) | `detail-page`, `list-page` |
| `kpi-card` | `kpi-card.tsx` | Active threads / moderation actions / unread mentions | `dashboard-page` |

### L1 primitives consumed (informational; not in spec `l2_blocks_used:`)

| L1 Primitive | File | Usage |
|---|---|---|
| `rich-text-editor` | `templates/L1/components/rich-text-editor.tsx` | Post body composer (sanitize fires on server submit) |
| `markdown-renderer` | `templates/L1/components/markdown-renderer.tsx` | Rendering of post / comment body (post-sanitize) |
| `relative-time` | `templates/L1/components/relative-time.tsx` | "2h ago" timestamps in thread list |

L1 primitives are excluded from the recipe spec's `l2_blocks_used:` list because
the `recipe_spec_referential_integrity_guard.sh` resolves `l2_blocks_used:`
entries against `templates/L2/blocks/<name>.tsx` only. L1 primitives are documented
here for AI implementers but not gated by the guard (same pattern as
`booking-recipe-l0.yaml` exclusion of `calendar` / `date-range-picker` / `relative-time`).

## Composition Order

```
list-page (threads)
  ├── search-input           ← full-text search
  ├── filter-bar             ← status / tag / author chips
  └── crud-list-adapter      ← paginated thread rows with reply count + status

list-page (moderation queue — admin)
  ├── filter-bar             ← status / reporter filter
  ├── data-table             ← reportable rows with quick-action column
  └── confirm-dialog         ← per-action confirmation

create-page (post)
  └── crud-create-form       ← title + body (rich-text-editor L1) + tags

create-page (comment — nested into detail-page)
  └── crud-create-form       ← reply body + parent_id

edit-page (post / comment)
  └── crud-edit-form         ← body only; status mutation routes via moderation queue

detail-page (thread)
  ├── crud-list-adapter      ← nested comment chain
  ├── notification-list      ← thread-scoped reply notifications
  └── confirm-dialog         ← moderator action prompt (hide / lock thread)

dashboard-page
  ├── kpi-card × 3           ← active threads / moderation actions / unread mentions
  └── notification-list      ← cross-thread reply notifications
```

## Notes

- `search-input` debouncing and zero-state handled by L2 block defaults.
- `notification-bell` (global app-shell) drives unread counter; clicking opens
  `notification-list` on the dashboard or detail page.
- `confirm-dialog` text varies by moderation action (hide / lock / restore);
  audit-log row writes regardless of dialog branch.
- Rich-text body submit triggers server-side sanitize (INV-005); client never
  trusts the editor output for storage. The co-shipped
  `frontend/tests/recipes/community-sanitize.spec.ts` asserts a `<script>`
  payload is stripped end-to-end.

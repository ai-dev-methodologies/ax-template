# L2 Block Selection — lms

> Which existing L2 blocks to use and in what composition order.

## Block Inventory

All blocks listed here already exist at `templates/L2/blocks/`. No new L2 blocks
are introduced by this recipe.

| Block | File | Usage in recipe | L3 page |
|---|---|---|---|
| `crud-create-form` | `crud-create-form.tsx` | New course / new lesson / new enrollment form | `create-page` |
| `crud-edit-form` | `crud-edit-form.tsx` | Edit course content / lesson body | `edit-page` |
| `crud-list-adapter` | `crud-list-adapter.tsx` | Paginated course list + lesson list | `list-page` |
| `data-table` | `data-table.tsx` | Enrollment roster (instructor view) | `list-page` (admin) |
| `filter-bar` | `filter-bar.tsx` | Course status / instructor / tag filter | `list-page` |
| `kpi-card` | `kpi-card.tsx` | Active courses / pending reminders / completion rate | `dashboard-page` |
| `notification-bell` | `notification-bell.tsx` | Unread reminder badge in header | (global app shell) |
| `notification-list` | `notification-list.tsx` | Due-date reminder panel | `dashboard-page`, `detail-page` |
| `confirm-dialog` | `confirm-dialog.tsx` | Course archive / unenroll confirmation | `detail-page`, `list-page` |

### L1 primitives consumed (informational; not in spec `l2_blocks_used:`)

| L1 Primitive | File | Usage |
|---|---|---|
| `calendar` | `templates/L1/components/calendar.tsx` | Due-date picker for lesson scheduling |
| `date-range-picker` | `templates/L1/components/date-range-picker.tsx` | Course-schedule range (enrollment-open → close) |
| `relative-time` | `templates/L1/components/relative-time.tsx` | "Last activity 2h ago" timestamps on enrollment rows |
| `progress` | `templates/L1/components/progress.tsx` | Lesson-completion bar per enrollment (disk file is `progress.tsx`, NOT `progress-bar.tsx`) |

L1 primitives are excluded from the recipe spec's `l2_blocks_used:` list because
the `recipe_spec_referential_integrity_guard.sh` resolves `l2_blocks_used:`
entries against `templates/L2/blocks/<name>.tsx` only. L1 primitives are documented
here for AI implementers but not gated by the guard (same pattern as
`booking-recipe-l0.yaml` exclusion of `calendar` / `date-range-picker` / `relative-time`
and `community-recipe-l0.yaml` exclusion of `rich-text-editor` / `markdown-renderer`).

## Composition Order

```
list-page (courses)
  ├── filter-bar             ← status / instructor / tag chips
  └── crud-list-adapter      ← paginated course rows with enrollment count + status

list-page (enrollment roster — admin)
  ├── filter-bar             ← status / completion-rate filter
  ├── data-table             ← enrollment rows with quick-action column
  └── confirm-dialog         ← per-action confirmation (archive / unenroll)

create-page (course)
  └── crud-create-form       ← title + description + author + start/end dates
                                (date-range-picker L1 + calendar L1)

create-page (lesson — nested into course detail-page)
  └── crud-create-form       ← lesson title + body + due-date
                                (calendar L1)

edit-page (course / lesson)
  └── crud-edit-form         ← content + due-date; visibility-state via separate
                                gated transition route

detail-page (course)
  ├── crud-list-adapter      ← nested lesson list with progress L1 bar per lesson
  ├── notification-list      ← course-scoped reminder notifications
  └── confirm-dialog         ← archive / unenroll prompt

dashboard-page
  ├── kpi-card × 3           ← active courses / pending reminders / completion rate
  └── notification-list      ← cross-course reminder notifications
```

## Notes

- `notification-bell` (global app-shell) drives unread reminder counter; clicking
  opens `notification-list` on the dashboard or detail page.
- `confirm-dialog` text varies by action (archive course / unenroll learner);
  audit-log row writes regardless of dialog branch.
- Due-date reminder fanout is driven server-side by the scheduled-task cron
  (`DueDateReminderTask`) — the L2 surface only shows the *result* of fanout
  (notification rows). The cron + lock + JobHistory live entirely in the
  `scheduled-task` L4 domain.
- Bulk-enrollment idempotency (LMS-INV-005) is enforced at the controller via
  the `Idempotency-Key` header (`practices/rules/idempotency-key-on-mutations.md`);
  the L2 form layer only forwards the header — dedupe is a backend concern.

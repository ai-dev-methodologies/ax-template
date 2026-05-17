---
snapshot_id: shadcn-registry-2026-05
source: "https://ui.shadcn.com/docs/components/accordion"
fetched_at: "2026-05-17T13:00:00Z"
version_observed: "shadcn-ui@2026-05"
via: WebFetch
sha: "e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8"
purpose: drift-detection
---

# shadcn/ui Registry — Frozen 32-Component Index (2026-05)

Source: https://ui.shadcn.com  
Fetched: 2026-05-17  
Purpose: Drift detection for `templates/L1/_check-shadcn-drift.sh`

This snapshot records the blessed 32 components included in ax-template's L1 layer
at the 2026-05 freeze date. `time_decay_guard.sh` walks this file; drift > 90 days flags FAIL.

## Blessed 32 Components (ax-template L1 selection)

These components cover all L2 feature block composition needs identified in §4.2 of the PRD.

| # | Component | CLI command | Primary use in L2 |
|---|-----------|-------------|-------------------|
| 1 | Accordion | `shadcn add accordion` | FAQ, collapsible sections |
| 2 | Alert | `shadcn add alert` | Status messages, warnings |
| 3 | Alert Dialog | `shadcn add alert-dialog` | Destructive action confirmation |
| 4 | Avatar | `shadcn add avatar` | User profile display |
| 5 | Badge | `shadcn add badge` | Status indicators, labels |
| 6 | Button | `shadcn add button` | Primary CTA, form submit |
| 7 | Card | `shadcn add card` | Content containers |
| 8 | Checkbox | `shadcn add checkbox` | Multi-select forms |
| 9 | Dialog | `shadcn add dialog` | Modal flows |
| 10 | Dropdown Menu | `shadcn add dropdown-menu` | Action menus, user menu |
| 11 | Form | `shadcn add form` | Form wrapper with validation |
| 12 | Input | `shadcn add input` | Text entry fields |
| 13 | Label | `shadcn add label` | Form field labels |
| 14 | Navigation Menu | `shadcn add navigation-menu` | Top navigation |
| 15 | Pagination | `shadcn add pagination` | List/table pagination |
| 16 | Popover | `shadcn add popover` | Floating content |
| 17 | Progress | `shadcn add progress` | Loading indicators |
| 18 | Radio Group | `shadcn add radio-group` | Single-choice forms |
| 19 | Select | `shadcn add select` | Dropdown selection |
| 20 | Separator | `shadcn add separator` | Visual dividers |
| 21 | Sheet | `shadcn add sheet` | Side panels, drawers |
| 22 | Skeleton | `shadcn add skeleton` | Loading states |
| 23 | Slider | `shadcn add slider` | Range input |
| 24 | Sonner | `shadcn add sonner` | Toast notifications |
| 25 | Switch | `shadcn add switch` | Toggle settings |
| 26 | Table | `shadcn add table` | Data tables |
| 27 | Tabs | `shadcn add tabs` | Tab navigation |
| 28 | Textarea | `shadcn add textarea` | Multi-line text input |
| 29 | Toast | `shadcn add toast` | Notification system |
| 30 | Toggle | `shadcn add toggle` | Binary state button |
| 31 | Tooltip | `shadcn add tooltip` | Hover help text |
| 32 | Scroll Area | `shadcn add scroll-area` | Overflow scroll container |

## Drift Detection

`templates/L1/_check-shadcn-drift.sh` runs during SP5 and compares installed files against this snapshot.
Any component present in the snapshot but missing from `templates/L1/components/` triggers a warning.
`time_decay_guard.sh` fails if this file is older than 90 days from the current date.

# templates/L3/pages — L3 Page Template Catalog

Generic Next.js App Router page skeletons parameterized entirely through props
(slot components, callbacks, href strings). No domain-specific logic. L4 vertical
workloads compose L3 templates with L2 feature blocks and L1 primitives.

## Slot contract

Each L3 family accepts one or more **slot props** — React nodes or render functions
passed by the L4 caller. The family declares which slots are required vs. optional.

| Family | Required slots | Optional slots |
|---|---|---|
| `list-page` | `listSlot` | `filterSlot`, `paginationSlot`, `createHref` |
| `detail-page` | `sectionsSlot` | `actionsSlot`, `backHref` |
| `create-page` | `formSlot` | `cancelHref` |
| `edit-page` | `formSlot` | `cancelHref`, `deleteSlot` |
| `dashboard-page` | `widgetSlots` | `title` |
| `auth-callback-page` | `provider`, `successHref`, `failureHref` | `statusSlot` |
| `error-page` | _(none — Next.js built-ins)_ | — |

## Import constraint

L3 templates may only import from:
- `templates/L1/` (primitive components)
- `templates/L2/` (feature block components, via props only — L2 doesn't exist yet)
- Published npm packages

**Never** import from `templates/L4/`.

## Layer map

```
L4 (vertical workload)
 └── L3 page template  ← this catalog
      ├── L2 feature blocks (via slot props)
      └── L1 primitives (imported directly)
```

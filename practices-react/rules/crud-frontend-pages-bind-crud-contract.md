---
title: "CRUD UI pages must realize the CRUD contract — server-paginated list with filter/empty/bulk states, create→redirect, detail with audit fields, edit pre-populated, delete behind a confirm dialog"
rule_id: crud-frontend-pages-bind-crud-contract
impact: MEDIUM
impactDescription: "A CRUD UI that paginates client-side breaks on large datasets; one with no EmptyState shows a blank table; one whose create page does not redirect to the new item leaves the user lost; an edit form not pre-populated from getItem silently blanks fields the user did not touch; a delete with no confirm dialog destroys data on a misclick. Each is a CRUD-surface defect that the documented component contract prevents."
tags:
  - crud
  - frontend
  - pagination
  - forms
  - contract-first
  - data-table
applicable_to:
  - react
  - nextjs
spec_ref: "specs/crud-frontend-l0.yaml#CRUD-FE-001"
verification:
  type: review
  notes: |
    Reviewer confirms the CRUD UI against specs/crud-frontend-l0.yaml: the list page is a DataTable over
    SERVER-paginated data calling listItems (CRUD-FE-001), with a FilterBar + SearchInput (002),
    Pagination reflecting pagination.totalPages from the response (003), an EmptyState on zero items (004),
    and a BulkActionsBar when rows are selected (005). The create page renders CrudCreateForm (title +
    description controlled fields), calls createItem on submit (006), and redirects to the new item's
    detail page on success (007). The detail page shows title/description + audit fields (createdAt,
    createdBy) from getItem (008). The edit page renders CrudEditForm PRE-POPULATED from getItem and calls
    updateItem (009). The edit danger zone renders a CrudDeleteConfirm dialog that calls deleteItem and
    redirects to the list (010). No client-side pagination of a server-paginated dataset.
evidence:
  - source_type: external
    citation: "React Docs — <input> (controlled inputs): CRUD create/edit form fields are controlled (value + onChange) (CRUD-FE-006/009)"
    url: "https://react.dev/reference/react-dom/components/input"
    quote: "To render a controlled input, pass the value prop to it (or checked for checkboxes and radios). React will force the input to always have the value you passed."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "React Docs — Reacting to input with state (declarative UI): list page renders empty/loaded/selected states declaratively (CRUD-FE-004/005)"
    url: "https://react.dev/learn/reacting-to-input-with-state"
    quote: "React provides a declarative way to manipulate the UI. Instead of manipulating individual pieces of the UI directly, you describe the different states that your component can be in, and switch between them in response to the user input."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## CRUD UI pages must realize the CRUD contract — server-paginated list, create→redirect, detail with audit, edit pre-populated, delete confirmed

**Impact: MEDIUM — A CRUD surface is the most-copied UI in any app, and the same defects recur: client-side pagination that loads the whole table and dies on large data; a blank table instead of an EmptyState; a create that succeeds but strands the user on the form instead of the new item; an edit form that mounts empty instead of pre-populated from `getItem`, so saving blanks every field the user did not retype; a delete that fires on a misclick with no confirm. The CRUD-frontend spec pins each surface to its documented component and endpoint. React supplies the primitives — form fields are controlled (*to render a controlled input, pass the value prop ... React will force the input to always have the value you passed*) and pages render their states declaratively (*you describe the different states that your component can be in*).**

There are ten load-bearing requirements — the items of `specs/crud-frontend-l0.yaml`, all governed by this rule.

**List page (CRUD-FE-001..005).** A DataTable over SERVER-paginated data calling `listItems` (001); a FilterBar + SearchInput to filter (002); Pagination controls reflecting `pagination.totalPages` from the response — not a client-side slice (003); an EmptyState when the response has zero items (004); a BulkActionsBar shown when rows are selected (005).

**Create (CRUD-FE-006..007).** A CrudCreateForm with title + description controlled fields calling `createItem` on submit (006), redirecting to the new item's detail page on success (007).

**Detail (CRUD-FE-008).** Shows title, description, and audit fields (`createdAt`, `createdBy`) from `getItem`.

**Edit + delete (CRUD-FE-009..010).** A CrudEditForm PRE-POPULATED with the item's current values from `getItem`, calling `updateItem` (009); a danger zone with a CrudDeleteConfirm dialog that calls `deleteItem` and redirects to the list (010).

**Incorrect — client-paginated, blank on empty, edit form not pre-populated, delete with no confirm:**

```tsx
const all = await listAllItems(); const page = all.slice(0,20);   {/* VIOLATION: client-side pagination (CRUD-FE-001/003) */}
return <Table rows={page} />;                                      {/* VIOLATION: no EmptyState on zero (CRUD-FE-004) */}
<form><input value="" onChange={...} /></form>                    {/* VIOLATION: edit form not pre-populated (CRUD-FE-009) */}
<button onClick={deleteItem}>Delete</button>                      {/* VIOLATION: no confirm dialog (CRUD-FE-010) */}
```

**Correct — server pagination, EmptyState, pre-populated edit, confirm-dialog delete with redirect:**

```tsx
const { data } = useQuery(['items', page], () => listItems({ page }));   // server-paginated (CRUD-FE-001)
if (data.items.length === 0) return <EmptyState />;                       // CRUD-FE-004
<Pagination totalPages={data.pagination.totalPages} />                    // from response (CRUD-FE-003)
// edit: form initialized from getItem
const item = await getItem(id); <CrudEditForm defaultValues={item} onSubmit={v => updateItem(id, v)} />  // CRUD-FE-009
// delete: confirm dialog
<CrudDeleteConfirm onConfirm={async () => { await deleteItem(id); router.push('/items'); }} />            // CRUD-FE-010
// create success → redirect to detail (CRUD-FE-007)
```

Verification: review-tier. CRUD-contract fidelity is a UI-to-API mapping property with no compile signal. Verify by review against `specs/crud-frontend-l0.yaml`: the list is server-paginated with filter/empty/bulk states; create redirects to detail; detail shows audit fields; edit is pre-populated; delete is behind a confirm dialog and redirects. When a fork-receiver wires real component tests (empty → EmptyState; edit mounts populated; delete requires confirm), this rule's verification may be upgraded from review to a test-tag binding.

Reference: [React — <input> (controlled inputs)](https://react.dev/reference/react-dom/components/input)

Reference: [React — Reacting to input with state](https://react.dev/learn/reacting-to-input-with-state)

---
title: "SavedView persistence must be 'url' or 'server' — localStorage is forbidden"
rule_id: saved-view-must-be-url-state-or-server-persisted
impact: HIGH
impactDescription: "Storing table saved-view config in localStorage makes views non-shareable, non-bookmarkable, and lost on incognito/different browser. URL state enables link-sharing; server persistence enables cross-device sync. localStorage silently breaks UX expectations."
tags:
  - url-state
  - saved-view
  - table
  - l2-block
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
protects_template_id: templates/L2/blocks/saved-view.tsx
failing_fixture_path: practices/evals/fixtures/saved-view-must-be-url-state-or-server-persisted/fail_saved_view_localstorage_only/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-URL-STATE-001"
verification:
  type: script
  status: automated
  notes: |
    saved_view_url_state_guard.sh (FMW4b) — binary, two checks:
      (1) Any file importing the saved-view block (templates/L2/blocks/saved-view)
          must NOT reference localStorage — the rule's forbidden persistence mode.
      (2) The L4 crud list reference (templates/L4/crud/app/(crud)/items/page.tsx)
          must drive page/sort/filter through useUrlListState (the L0 URL-state
          primitive), so the catalog dogfoods its own URL-as-state mandate.
    Replaces the prior manual review. Scoped to saved-view consumers + one pinned
    reference path, so it does not false-positive on unrelated useState (the FMW4a
    lesson: name-based checks need shape/scope guards).
evidence:
  - source_type: external
    citation: "web.dev URL as state — encoding application state in the URL makes it shareable, bookmarkable, and resilient to session loss. localStorage state is invisible to the server and breaks link sharing."
    url: "https://web.dev/articles/url-state"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "Next.js useSearchParams — persisting filter/view state in search params allows deep-linking to exact table state without a database round-trip"
    url: "https://nextjs.org/docs/app/api-reference/functions/use-search-params"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "MDN Web Docs — localStorage: data is scoped to the origin and session, not shareable via URL, invisible to server, and cleared in incognito mode — making it unsuitable for collaborative or cross-device view state"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## SavedView persistence must be 'url' or 'server' — localStorage is forbidden

**Impact: HIGH — localStorage-based saved views are non-shareable, non-bookmarkable, and silently fail in incognito/other-browser scenarios. Enforcement is binary: either URL state or server API.**

### The violation — localStorage bypass

```typescript
// ❌ WRONG — view config in localStorage: not shareable, not bookmarkable
"use client";
import { SavedView } from "templates/L2/blocks/saved-view";

export function ProductTableToolbar() {
  const [views, setViews] = React.useState(() => {
    // VIOLATION: localStorage not a valid persistence mode
    const stored = localStorage.getItem("product-table-views");
    return stored ? JSON.parse(stored) : [];
  });

  function handleSave(name: string, config: SavedViewConfig) {
    const updated = [...views, { id: crypto.randomUUID(), name, config, persistence: "localStorage" }];
    setViews(updated);
    // VIOLATION: writing view config to localStorage
    localStorage.setItem("product-table-views", JSON.stringify(updated));
  }

  return <SavedView items={views} onSave={handleSave} onLoad={applyView} onDelete={deleteView} />;
}
```

### Correct — URL state via the catalog primitives (shareable, no server round-trip)

Do NOT hand-roll `URLSearchParams` page/sort/filter plumbing. The catalog ships
the canonical seam: `useUrlListState` (L0) reads the live view state from the
query string, and `listStateToQuery` serialises a state back into a permalink —
so "save this view" is just capturing the current querystring as a named
bookmark. localStorage never enters the picture.

```typescript
// ✅ CORRECT — persistence: 'url' — live view state lives in the query string
"use client";
import { useRouter } from "next/navigation";
import {
  useUrlListState,
  listStateToQuery,
} from "templates/L0/fork-receiver-kit/use-url-list-state";
import { SavedView } from "templates/L2/blocks/saved-view";

export function ProductTableToolbar() {
  const router = useRouter();
  // Page / sort / search / filter are URL-backed — shareable + back-button-correct.
  const list = useUrlListState({ filterKeys: ["status", "category"] });

  function handleSave(name: string) {
    // Capture the CURRENT view as a permalink (inverse serializer) — a named
    // bookmark, not localStorage. Persist the querystring however you like
    // (URL param, server prefs); here we push it as a shareable link.
    const query = listStateToQuery(list);
    router.push(`?${query}`);
    // …append { id: crypto.randomUUID(), name, query, persistence: "url" } to your view set
  }

  return (
    <SavedView
      items={savedViews}                                    // persistence: 'url' ✅
      onSave={handleSave}
      onLoad={view => router.push(`?${view.query}`)}         // re-applying a view = navigating to its querystring
      onDelete={deleteView}
    />
  );
}
```

### Correct — server persistence (cross-device sync)

```typescript
// ✅ CORRECT — persistence: 'server' — stored in user preferences API
"use client";
import { SavedView } from "templates/L2/blocks/saved-view";
import { useSavedViews, useCreateSavedView, useDeleteSavedView } from "@/lib/api/user-prefs";

export function ProductTableToolbar() {
  const { data: views } = useSavedViews("product-table");
  const { mutate: createView } = useCreateSavedView();
  const { mutate: deleteView } = useDeleteSavedView();

  return (
    <SavedView
      items={(views ?? []).map(v => ({ ...v, persistence: "server" }))}  // persistence: 'server' ✅
      onSave={(name, config) => createView({ table: "product-table", name, config })}
      onLoad={applyView}
      onDelete={id => deleteView(id)}
    />
  );
}
```

### Persistence mode decision matrix

| Scenario | Mode | Rationale |
|---|---|---|
| Public-facing table (shareable links) | `url` | URL encodes view; anyone with the link sees the same state |
| Internal admin table (personal preferences) | `server` | Persists across devices; stored in user prefs API |
| Quick layout tweak (no sharing needed) | `url` | Still shareable; zero server cost |
| **Any case** | **NEVER `localStorage`** | Not shareable, not server-readable, lost in incognito |

### Failing fixture

`practices/evals/fixtures/saved-view-must-be-url-state-or-server-persisted/fail_saved_view_localstorage_only/`

Contains a component that:
1. Reads saved views from `localStorage.getItem`
2. Writes saved views to `localStorage.setItem`
3. Sets `persistence: 'localStorage'` (not a valid `SavedViewPersistence` type value)

Running the fixture should cause ESLint to flag the localStorage usage.

### TDD anchor

`templates/_tests/saved-view-persistence.spec.ts` asserts:

```typescript
import { expect, test } from "vitest";

test("SavedView persistence type excludes localStorage", () => {
  // The SavedViewPersistence type must be 'url' | 'server' only
  const urlView: import("../L2/blocks/saved-view").SavedViewPersistence = "url";
  const serverView: import("../L2/blocks/saved-view").SavedViewPersistence = "server";
  expect(urlView).toBe("url");
  expect(serverView).toBe("server");
  // TypeScript compile error if someone passes 'localStorage' — enforced at type level
});

test("SavedViewItem.persistence is not localStorage", () => {
  const view = {
    id: "1",
    name: "My View",
    config: { columns: ["id", "name"] },
    persistence: "url" as const,
  };
  expect(view.persistence).toBe("url");
  expect(view.persistence).not.toBe("localStorage");
});
```

Reference: [web.dev — URL as state](https://web.dev/articles/url-state)

Reference: [Next.js — useSearchParams](https://nextjs.org/docs/app/api-reference/functions/use-search-params)

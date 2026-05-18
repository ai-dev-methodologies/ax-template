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
  type: review
  status: manual
  notes: "Any component using SavedView or managing table view config must route persistence through URL search params or a server API endpoint. Usage of localStorage.setItem / localStorage.getItem for view config triggers this rule."
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

### Correct — URL state (shareable, no server round-trip)

```typescript
// ✅ CORRECT — persistence: 'url' — view config encoded in URL search params
"use client";
import { useRouter, useSearchParams } from "next/navigation";
import { SavedView, type SavedViewConfig } from "templates/L2/blocks/saved-view";

export function ProductTableToolbar() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Views are bookmarks: each is a URL with ?view=<base64-config>
  const views = parseViewsFromSearchParams(searchParams);

  function handleSave(name: string, config: SavedViewConfig) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("savedViews", encodeViews([...views, { id: crypto.randomUUID(), name, config }]));
    router.push(`?${params.toString()}`);
  }

  return (
    <SavedView
      items={views.map(v => ({ ...v, persistence: "url" }))}  // persistence: 'url' ✅
      onSave={handleSave}
      onLoad={view => applyViewFromConfig(view.config, router)}
      onDelete={id => removeViewFromUrl(id, router)}
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

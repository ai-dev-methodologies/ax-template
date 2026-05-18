/**
 * FIXTURE: saved-view-must-be-url-state-or-server-persisted/pass
 * Demonstrates CORRECT pattern: saved view config stored via URL state.
 * URL persistence is shareable, bookmarkable, and works in incognito.
 */
"use client";

import React from "react";

// Simulating the SavedView type — fixture doesn't import the actual L2 block
interface SavedViewItem {
  id: string;
  name: string;
  config: { columns: string[]; sort?: { field: string; direction: "asc" | "desc" } };
  // CORRECT: persistence is 'url' or 'server' only
  persistence: "url" | "server";
}

export default function ProductTableToolbar() {
  // CORRECT: views derived from URL search params (shareable, bookmarkable)
  const searchParams =
    typeof window !== "undefined"
      ? new URLSearchParams(window.location.search)
      : new URLSearchParams();

  const views: SavedViewItem[] = React.useMemo(() => {
    const raw = searchParams.get("savedViews");
    if (!raw) return [];
    try {
      return JSON.parse(atob(raw));
    } catch {
      return [];
    }
  }, [searchParams.toString()]);

  function handleSave(name: string) {
    const newView: SavedViewItem = {
      id: crypto.randomUUID(),
      name,
      config: { columns: ["id", "name", "status"] },
      // CORRECT: persistence mode is 'url'
      persistence: "url",
    };
    const updated = [...views, newView];
    // CORRECT: persisting to URL search params, not localStorage
    const params = new URLSearchParams(window.location.search);
    params.set("savedViews", btoa(JSON.stringify(updated)));
    window.history.pushState({}, "", `?${params.toString()}`);
  }

  function handleDelete(id: string) {
    const updated = views.filter((v) => v.id !== id);
    const params = new URLSearchParams(window.location.search);
    if (updated.length === 0) {
      params.delete("savedViews");
    } else {
      params.set("savedViews", btoa(JSON.stringify(updated)));
    }
    window.history.pushState({}, "", `?${params.toString()}`);
  }

  return (
    <div>
      <button type="button" onClick={() => handleSave("My View")}>
        Save view
      </button>
      <ul>
        {views.map((v) => (
          <li key={v.id}>
            {v.name} ({v.persistence})
            <button type="button" onClick={() => handleDelete(v.id)}>
              Delete
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

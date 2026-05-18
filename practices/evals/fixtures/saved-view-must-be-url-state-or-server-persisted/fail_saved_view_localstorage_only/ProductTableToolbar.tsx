/**
 * FIXTURE: saved-view-must-be-url-state-or-server-persisted/fail_saved_view_localstorage_only
 * Demonstrates WRONG pattern: saved view config stored in localStorage.
 * localStorage is not shareable, not bookmarkable, and lost in incognito.
 * Guard must catch: localStorage.setItem / localStorage.getItem used for view persistence.
 */
"use client";

import React from "react";

// Simulating the SavedView type — fixture doesn't import the actual L2 block
interface SavedViewItem {
  id: string;
  name: string;
  config: { columns: string[]; sort?: { field: string; direction: "asc" | "desc" } };
  // VIOLATION: 'localStorage' is not a valid SavedViewPersistence value
  persistence: "url" | "server" | "localStorage";
}

const STORAGE_KEY = "product-table-views";

export default function ProductTableToolbar() {
  const [views, setViews] = React.useState<SavedViewItem[]>(() => {
    // VIOLATION: reading saved views from localStorage
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  });

  function handleSave(name: string) {
    const newView: SavedViewItem = {
      id: crypto.randomUUID(),
      name,
      config: { columns: ["id", "name", "status"] },
      // VIOLATION: persistence mode is 'localStorage'
      persistence: "localStorage",
    };
    const updated = [...views, newView];
    setViews(updated);
    // VIOLATION: writing view config to localStorage
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
  }

  function handleDelete(id: string) {
    const updated = views.filter((v) => v.id !== id);
    setViews(updated);
    // VIOLATION: writing to localStorage
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
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

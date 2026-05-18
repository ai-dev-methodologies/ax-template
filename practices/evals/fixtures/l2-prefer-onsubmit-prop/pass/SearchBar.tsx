/**
 * FIXTURE: l2-prefer-onsubmit-prop/pass
 * Demonstrates CORRECT pattern: L2 block receives onSubmit as a prop.
 * No server action import. The L4 page wires the action.
 */
"use client";

import { useState } from "react";

interface SearchBarProps {
  placeholder?: string;
  // CORRECT: caller (L4 page) provides the submit handler — no domain coupling
  onSubmit: (query: string) => void | Promise<void>;
}

export default function SearchBar({ placeholder = "Search...", onSubmit }: SearchBarProps) {
  const [query, setQuery] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    await onSubmit(query);
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder={placeholder}
      />
      <button type="submit">Search</button>
    </form>
  );
}

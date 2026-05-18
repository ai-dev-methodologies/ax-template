/**
 * FIXTURE: l2-prefer-onsubmit-prop/fail_server_action_import
 * Demonstrates WRONG pattern: L2 block importing a server action directly.
 * This binds the L2 block to a specific domain endpoint, making it
 * unreusable and untestable without the backend.
 * Guard must catch: import from "@/lib/actions" or "app/..." in an L2 block.
 */
"use client";

// VIOLATION: L2 blocks must not import server actions — this couples the block
// to the specific domain and breaks layer decoupling.
import { searchProducts } from "@/lib/actions/search";
import { useState } from "react";

interface SearchBarProps {
  placeholder?: string;
}

// BUG: fetches its own data by importing a server action directly.
// Cannot be reused for user search, order search, or any other domain.
export default function SearchBar({ placeholder = "Search..." }: SearchBarProps) {
  const [query, setQuery] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    // VIOLATION: directly calls imported server action
    const results = await searchProducts(query);
    console.log(results);
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

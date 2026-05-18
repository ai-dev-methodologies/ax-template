/*
---
template_id: L4/search/app/(search)/page.tsx
layer: L4
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js App Router — Route Groups: Parentheses in directory names create route groups that don't affect the URL path but allow shared layouts."
    url: "https://nextjs.org/docs/app/building-your-application/routing/route-groups"
  - source_type: external
    citation: "WCAG 2.2 — 3.2.3 Consistent Navigation: Navigational mechanisms that appear on multiple pages must occur in the same relative order."
    url: "https://www.w3.org/TR/WCAG22/#consistent-navigation"
imports_from: [L1, L2, L3]
imports_forbidden: []
---
*/
import * as React from 'react'
import SearchPalette from '@/components/search/SearchPalette'
import RecentSearches, { useRecentSearches } from '@/components/search/RecentSearches'

/**
 * Search landing page — entry point for the (search) route group.
 *
 * URL: /search
 *
 * Renders the Cmd+K palette trigger and recent search history.
 * Submitting a query navigates to /search/results?q=…
 *
 * Fork instructions:
 *   1. Copy the entire templates/L4/search/ tree to your app/ directory.
 *   2. Adjust imports to your project's actual component paths.
 *   3. Replace the router.push navigation with your routing strategy if not using Next.js.
 */
export default function SearchPage() {
  return (
    <main className="container mx-auto px-4 py-16 max-w-2xl text-center">
      <h1 className="text-3xl font-bold mb-4">검색</h1>
      <p className="text-muted-foreground mb-8 text-sm">
        Cmd+K 또는 아래 입력창으로 검색하세요.
      </p>

      {/*
        SearchPalette: Cmd+K trigger + IME-safe input.
        In a real fork, pass onSearch with router.push:
          onSearch={(q) => router.push(`/search/results?q=${encodeURIComponent(q)}`)}
      */}
      <SearchPaletteIsland />
    </main>
  )
}

/**
 * Client island for search palette — needed because SearchPalette is a Client Component
 * while this page is a Server Component.
 */
'use client'
function SearchPaletteIsland() {
  const recent = useRecentSearches()

  function handleSearch(q: string) {
    recent.add(q)
    window.location.href = `/search/results?q=${encodeURIComponent(q)}`
  }

  return (
    <div className="flex flex-col items-center gap-6">
      <SearchPalette
        onSearch={handleSearch}
        placeholder="무엇을 찾으시나요?"
        className="w-full max-w-md"
      />
      <RecentSearches
        onSelect={handleSearch}
        className="w-full max-w-md text-left"
      />
    </div>
  )
}

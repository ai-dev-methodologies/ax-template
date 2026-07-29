/*
---
template_id: L4/search/app/(search)/search-landing-view
layer: L4
domain: search
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (search)/page.tsx (BACKLOG P2-42
      render-testability pass-1 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      the page's interactive Cmd+K trigger (SearchPalette) statically imports the external npm
      package 'cmdk', which is installed under frontend/node_modules but unreachable from a
      templates/L2/blocks/ file living outside the frontend/ project root — the SAME documented
      class of gap as frontend/tests/L2/search-palette-hydration.spec.ts and
      frontend/tests/audit-log-redaction-render.vitest.tsx's @tanstack/react-query note. This view
      therefore takes the already-instantiated palette as a `paletteSlot` prop (caller owns the
      cmdk-dependent element) and renders templates/L2/blocks/recent-searches directly — that
      block has zero external-npm deps, so it is safe to import and render here."
---
*/
import * as React from 'react'
import RecentSearches from 'templates/L2/blocks/recent-searches'

// ─── component ──────────────────────────────────────────────────────────────

export interface SearchLandingViewProps {
  /** The already-instantiated Cmd+K palette trigger — kept out of this file because
   *  SearchPalette's 'cmdk' dependency cannot resolve when imported from a vitest that
   *  renders this view directly (see evidence rationale above). */
  paletteSlot: React.ReactNode
  /** Fired when the caller selects a recent search term. Shares the same handler the
   *  caller wires to paletteSlot's own onSearch, so both surfaces navigate identically. */
  onSelectRecent: (query: string) => void
}

/**
 * SearchLandingView — pure presentational render of the /search landing page.
 *
 * Deliberately has ZERO dependency on 'cmdk' — the caller (`(search)/page.tsx`'s client
 * island) owns the SearchPalette instantiation and passes the rendered element in via
 * `paletteSlot`. This keeps the component a plain props -> JSX function, which is what
 * makes it renderable in a unit test without the frontend/ project's own dependency graph.
 */
export default function SearchLandingView({ paletteSlot, onSelectRecent }: SearchLandingViewProps) {
  return (
    <main className="container mx-auto px-4 py-16 max-w-2xl text-center">
      <h1 className="text-3xl font-bold mb-4">검색</h1>
      <p className="text-muted-foreground mb-8 text-sm">
        Cmd+K 또는 아래 입력창으로 검색하세요.
      </p>

      <div className="flex flex-col items-center gap-6">
        {paletteSlot}
        <RecentSearches onSelect={onSelectRecent} className="w-full max-w-md text-left" />
      </div>
    </main>
  )
}

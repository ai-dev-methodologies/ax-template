/*
---
template_id: L3/pages/search-results-page/loading
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js App Router — loading.js: Special file that creates a React Suspense boundary. Shown immediately while the page segment loads, replaced when data is ready."
    url: "https://nextjs.org/docs/app/api-reference/file-conventions/loading"
imports_from: [L1, L2, L3]
imports_forbidden: [lib/auth/]
---
*/

/**
 * SearchResultsPage loading skeleton — shown during Suspense while fetchSearchResults resolves.
 *
 * Fork instructions:
 *   Copy alongside page.tsx. Next.js picks it up automatically.
 */
export default function SearchResultsLoading() {
  return (
    <main className="container mx-auto px-4 py-8 max-w-3xl animate-pulse">
      {/* Search bar skeleton */}
      <div className="h-10 rounded-lg bg-muted mb-6 max-w-xl" />

      {/* Summary line */}
      <div className="h-4 w-40 rounded bg-muted mb-4" />

      {/* Result skeletons */}
      <ol className="space-y-6" aria-label="검색 결과 불러오는 중" aria-busy="true">
        {Array.from({ length: 5 }).map((_, i) => (
          <li key={i} className="border-b pb-6 last:border-0 space-y-2">
            <div className="h-5 w-3/4 rounded bg-muted" />
            <div className="h-4 w-full rounded bg-muted" />
            <div className="h-4 w-5/6 rounded bg-muted" />
          </li>
        ))}
      </ol>
    </main>
  )
}

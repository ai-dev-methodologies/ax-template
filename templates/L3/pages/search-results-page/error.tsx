/*
---
template_id: L3/pages/search-results-page/error
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js App Router — error.js: Error boundary for a route segment. Must be a Client Component ('use client') to access the error object and reset function."
    url: "https://nextjs.org/docs/app/api-reference/file-conventions/error"
imports_from: [L1, L2, L3]
imports_forbidden: [lib/auth/]
---
*/
'use client'

import * as React from 'react'

interface SearchResultsErrorProps {
  error: Error & { digest?: string }
  reset: () => void
}

/**
 * SearchResultsPage error boundary — shown when fetchSearchResults throws.
 *
 * Fork instructions:
 *   Copy alongside page.tsx. Next.js picks it up automatically.
 */
export default function SearchResultsError({ error, reset }: SearchResultsErrorProps) {
  React.useEffect(() => {
    // Log to your error reporting service here (e.g. Sentry)
    console.error('[SearchResultsPage] error boundary caught:', error)
  }, [error])

  return (
    <main className="container mx-auto px-4 py-8 max-w-3xl text-center">
      <p className="text-lg font-medium mb-2 text-destructive">검색 중 오류가 발생했습니다</p>
      <p className="text-sm text-muted-foreground mb-6">
        잠시 후 다시 시도해주세요.
      </p>
      <button
        type="button"
        onClick={reset}
        className="px-4 py-2 text-sm rounded-lg bg-primary text-primary-foreground hover:bg-primary/90"
      >
        다시 시도
      </button>
    </main>
  )
}

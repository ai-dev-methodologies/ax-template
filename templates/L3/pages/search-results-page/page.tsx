/*
---
template_id: L3/pages/search-results-page
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js App Router — searchParams: Page components in the App Router receive searchParams as a prop containing URL query parameters. Use for q, page, size parameters in search pages."
    url: "https://nextjs.org/docs/app/api-reference/file-conventions/page#searchparams-optional"
  - source_type: external
    citation: "WCAG 2.2 — 2.4.2 Page Titled: Web pages have titles that describe topic or purpose. Search results page must include query in the title."
    url: "https://www.w3.org/TR/WCAG22/#page-titled"
imports_from: [L1, L2, L3]
imports_forbidden: [lib/auth/]
---
*/
import * as React from 'react'
import TypeaheadSearch from '@/components/search/TypeaheadSearch'
import ResultHighlighter from '@/components/search/ResultHighlighter'
import RecentSearches, { useRecentSearches } from '@/components/search/RecentSearches'

// ─── types ────────────────────────────────────────────────────────────────────

interface SearchHit {
  id: string
  title: string
  snippet: string
  score: number
}

interface SearchResultPage {
  hits: SearchHit[]
  totalHits: number
  page: number
  size: number
  processingTimeMs: number
}

// ─── server fetch ─────────────────────────────────────────────────────────────

async function fetchSearchResults(
  query: string,
  page: number,
  size: number,
): Promise<SearchResultPage | null> {
  if (!query.trim()) return null
  try {
    const res = await fetch(
      `${process.env.NEXT_PUBLIC_API_BASE_URL ?? ''}/api/v1/search`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query, page, size }),
        next: { revalidate: 30 },
      },
    )
    if (!res.ok) return null
    return res.json() as Promise<SearchResultPage>
  } catch {
    return null
  }
}

// ─── page ─────────────────────────────────────────────────────────────────────

interface SearchResultsPageProps {
  searchParams: Promise<{ q?: string; page?: string; size?: string }>
}

/**
 * SearchResultsPage — Server Component page for paginated search results.
 *
 * URL params:
 *   - q    — search query string
 *   - page — 0-indexed page number (default: 0)
 *   - size — page size (default: 20, capped server-side at 100)
 *
 * Fork instructions:
 *   1. Copy to `app/(search)/results/page.tsx` in your fork.
 *   2. Replace imports with your project's component paths.
 *   3. Adjust fetch URL and authentication headers as needed.
 *   4. Add filters (category, date range, etc.) as additional searchParams.
 */
export default async function SearchResultsPage({ searchParams }: SearchResultsPageProps) {
  const params = await searchParams
  const query = params.q ?? ''
  const page = Math.max(0, parseInt(params.page ?? '0', 10) || 0)
  const size = Math.min(100, Math.max(1, parseInt(params.size ?? '20', 10) || 20))

  const results = await fetchSearchResults(query, page, size)

  const totalPages = results ? Math.ceil(results.totalHits / results.size) : 0
  const hasResults = results && results.hits.length > 0

  return (
    <main className="container mx-auto px-4 py-8 max-w-3xl">
      {/* Page title for accessibility (WCAG 2.4.2) */}
      <title>{query ? `'${query}' 검색 결과` : '검색'}</title>

      {/* Search input — client island */}
      <div className="mb-6">
        <TypeaheadSearch
          placeholder="검색어 입력…"
          onQuery={() => {/* handled by navigation in fork */}}
          className="max-w-xl"
        />
      </div>

      {/* Query summary */}
      {query && (
        <p className="text-sm text-muted-foreground mb-4">
          {results ? (
            <>
              <strong>{results.totalHits.toLocaleString()}</strong>개의 결과
              {' '}({results.processingTimeMs}ms)
            </>
          ) : (
            '결과 없음'
          )}
        </p>
      )}

      {/* Results list */}
      {hasResults && (
        <ol className="space-y-6" aria-label="검색 결과">
          {results.hits.map((hit) => (
            <li key={hit.id} className="border-b pb-6 last:border-0">
              <h2 className="text-base font-semibold mb-1">
                <ResultHighlighter text={hit.title} query={query} />
              </h2>
              <p className="text-sm text-muted-foreground leading-relaxed">
                <ResultHighlighter text={hit.snippet} query={query} maxLength={200} />
              </p>
            </li>
          ))}
        </ol>
      )}

      {/* Empty state */}
      {!hasResults && query && (
        <div className="text-center py-16 text-muted-foreground">
          <p className="text-lg font-medium mb-2">검색 결과가 없습니다</p>
          <p className="text-sm">&apos;{query}&apos;에 대한 결과를 찾을 수 없습니다.</p>
        </div>
      )}

      {/* Default state (no query) */}
      {!query && (
        <RecentSearches
          onSelect={() => {/* handled by navigation in fork */}}
          className="mt-4"
        />
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <nav className="flex items-center justify-center gap-2 mt-8" aria-label="페이지 이동">
          {page > 0 && (
            <a
              href={`?q=${encodeURIComponent(query)}&page=${page - 1}&size=${size}`}
              className="px-3 py-1.5 text-sm rounded border hover:bg-accent"
            >
              이전
            </a>
          )}
          <span className="text-sm text-muted-foreground">
            {page + 1} / {totalPages}
          </span>
          {page < totalPages - 1 && (
            <a
              href={`?q=${encodeURIComponent(query)}&page=${page + 1}&size=${size}`}
              className="px-3 py-1.5 text-sm rounded border hover:bg-accent"
            >
              다음
            </a>
          )}
        </nav>
      )}
    </main>
  )
}

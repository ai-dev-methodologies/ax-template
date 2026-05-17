/*
---
template_id: L3/pages/error-page/loading
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 15 App Router loading.tsx — instant loading states with React Suspense"
    url: "https://nextjs.org/docs/app/building-your-application/routing/loading-ui-and-streaming"
  - source_type: internal
    rationale: "Generic loading skeleton for ax-template L4. Wrapped automatically by Next.js Suspense boundary on navigation. No domain logic."
dependencies: []
---
*/

/**
 * Loading — Next.js App Router loading.tsx convention.
 *
 * This file is automatically wrapped by Next.js in a <Suspense> boundary
 * and shown during route segment loading. No props accepted per Next.js
 * convention. Customize the skeleton to match the page layout.
 *
 * L4 usage: Copy this file to your L4 route segment directory and adjust
 * the skeleton shape to match your page layout.
 */
export default function Loading() {
  return (
    <main className="container mx-auto px-4 py-8 space-y-6" aria-busy="true" aria-label="Loading">
      {/* Header skeleton */}
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-2">
          <div className="h-7 w-48 rounded-md bg-muted animate-pulse" />
          <div className="h-4 w-72 rounded bg-muted animate-pulse" />
        </div>
        <div className="h-9 w-24 rounded-md bg-muted animate-pulse" />
      </div>

      {/* Filter bar skeleton */}
      <div className="h-10 w-full rounded-md bg-muted animate-pulse" />

      {/* List skeleton — 5 rows */}
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <div
            key={i}
            className="h-14 w-full rounded-lg bg-muted animate-pulse"
            style={{ animationDelay: `${i * 50}ms` }}
          />
        ))}
      </div>

      {/* Pagination skeleton */}
      <div className="flex justify-center gap-2">
        <div className="h-9 w-9 rounded-md bg-muted animate-pulse" />
        <div className="h-9 w-9 rounded-md bg-muted animate-pulse" />
        <div className="h-9 w-9 rounded-md bg-muted animate-pulse" />
      </div>
    </main>
  )
}

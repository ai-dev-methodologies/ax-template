/*
---
template_id: L3/pages/pricing-page/loading
layer: L3
evidence:
  - source_type: external
    citation: "Next.js 15 App Router — loading.tsx creates an instant loading UI with React Suspense"
    url: "https://nextjs.org/docs/app/building-your-application/routing/loading-ui-and-streaming"
    quoted_at: "2026-05-18"
---
*/

export default function PricingPageLoading() {
  return (
    <main className="mx-auto max-w-6xl px-4 py-12 space-y-12" aria-busy aria-label="요금제 로딩 중">
      <div className="h-10 w-64 animate-pulse rounded-lg bg-muted mx-auto" />
      <div className="grid grid-cols-3 gap-6">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="h-80 animate-pulse rounded-2xl bg-muted" />
        ))}
      </div>
    </main>
  )
}

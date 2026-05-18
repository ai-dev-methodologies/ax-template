/*
---
template_id: L3/pages/pricing-page/error
layer: L3
evidence:
  - source_type: external
    citation: "Next.js 15 App Router — error.tsx creates an error boundary for the route segment"
    url: "https://nextjs.org/docs/app/building-your-application/routing/error-handling"
    quoted_at: "2026-05-18"
---
*/

'use client'

import * as React from 'react'

export default function PricingPageError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  return (
    <main className="mx-auto max-w-6xl px-4 py-24 text-center space-y-4">
      <h2 className="text-xl font-semibold">요금제를 불러올 수 없습니다</h2>
      <p className="text-muted-foreground text-sm">{error.message}</p>
      <button
        type="button"
        onClick={reset}
        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90"
      >
        다시 시도
      </button>
    </main>
  )
}

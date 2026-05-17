/*
---
template_id: L3/pages/detail-page
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 15 App Router dynamic routes — page.tsx with params"
    url: "https://nextjs.org/docs/app/building-your-application/routing/dynamic-routes"
  - source_type: internal
    rationale: "Generic detail-page skeleton for ax-template L4 composition. Accepts slot props for sections and actions — no domain logic."
dependencies: []
---
*/
import * as React from 'react'

/**
 * DetailPage — generic single-resource detail view template.
 *
 * Slot props:
 *   - title        (required) resource name / page heading
 *   - sectionsSlot (required) content sections (field groups, cards, etc.)
 *   - actionsSlot  (optional) action buttons (edit, delete, etc.)
 *   - backHref     (optional) if provided, renders a back link
 *   - backLabel    (optional) back link text (default: "Back")
 *
 * L4 usage:
 *   import DetailPage from 'templates/L3/pages/detail-page/[id]/page'
 *   export default async function ProductDetailPage({ params }) {
 *     const product = await fetchProduct(params.id)
 *     return (
 *       <DetailPage
 *         title={product.name}
 *         backHref="/products"
 *         actionsSlot={<ProductActions id={params.id} />}
 *         sectionsSlot={<ProductDetails product={product} />}
 *       />
 *     )
 *   }
 */
export interface DetailPageProps {
  /** Resource name shown as page heading */
  title: string
  /** Main content sections (field groups, info cards, tabs, etc.) */
  sectionsSlot: React.ReactNode
  /** Action buttons rendered in the header area */
  actionsSlot?: React.ReactNode
  /** If provided, renders a back navigation link */
  backHref?: string
  /** Back link label (default: "Back") */
  backLabel?: string
}

export default function DetailPage({
  title,
  sectionsSlot,
  actionsSlot,
  backHref,
  backLabel = 'Back',
}: DetailPageProps) {
  return (
    <main className="container mx-auto px-4 py-8 space-y-6">
      {/* Back link */}
      {backHref && (
        <nav aria-label="breadcrumb">
          <a
            href={backHref}
            className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            <span aria-hidden="true">←</span>
            {backLabel}
          </a>
        </nav>
      )}

      {/* Header row */}
      <div className="flex items-start justify-between gap-4">
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        {actionsSlot && (
          <div className="flex items-center gap-2">{actionsSlot}</div>
        )}
      </div>

      {/* Sections slot */}
      <div className="space-y-6">{sectionsSlot}</div>
    </main>
  )
}

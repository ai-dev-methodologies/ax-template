/*
---
template_id: L3/pages/edit-page
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 15 App Router dynamic routes — page.tsx with params"
    url: "https://nextjs.org/docs/app/building-your-application/routing/dynamic-routes"
  - source_type: internal
    rationale: "Generic edit-page skeleton for ax-template L4 composition. Accepts form slot with initial values and optional delete slot — no domain logic."
dependencies: []
---
*/
import * as React from 'react'

/**
 * EditPage — generic resource edit form template.
 *
 * Slot props:
 *   - title       (required) page heading
 *   - description (optional) subtitle text
 *   - formSlot    (required) pre-populated form (L2 form block)
 *   - deleteSlot  (optional) destructive action area (delete button + confirmation)
 *   - cancelHref  (optional) if provided, renders a Cancel link
 *   - cancelLabel (optional) cancel label (default: "Cancel")
 *
 * L4 usage:
 *   import EditPage from 'templates/L3/pages/edit-page/[id]/page'
 *   export default async function EditProductPage({ params }) {
 *     const product = await getProduct(params.id)
 *     return (
 *       <EditPage
 *         title={`Edit ${product.name}`}
 *         cancelHref={`/products/${params.id}`}
 *         formSlot={<ProductEditForm product={product} />}
 *         deleteSlot={<DeleteProductButton id={params.id} />}
 *       />
 *     )
 *   }
 */
export interface EditPageProps {
  /** Page heading */
  title: string
  /** Optional subtitle */
  description?: string
  /** Pre-populated form (required) — L2 form block with initial values */
  formSlot: React.ReactNode
  /** Destructive action area (optional) — shown in a danger zone section */
  deleteSlot?: React.ReactNode
  /** If provided, renders a Cancel link */
  cancelHref?: string
  /** Cancel link label (default: "Cancel") */
  cancelLabel?: string
}

export default function EditPage({
  title,
  description,
  formSlot,
  deleteSlot,
  cancelHref,
  cancelLabel = 'Cancel',
}: EditPageProps) {
  return (
    <main className="container mx-auto px-4 py-8 max-w-2xl space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          {description && (
            <p className="text-sm text-muted-foreground">{description}</p>
          )}
        </div>
        {cancelHref && (
          <a
            href={cancelHref}
            className="text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            {cancelLabel}
          </a>
        )}
      </div>

      {/* Form slot */}
      <div className="rounded-lg border bg-card p-6 shadow-sm">
        {formSlot}
      </div>

      {/* Danger zone — only rendered when deleteSlot is provided */}
      {deleteSlot && (
        <div className="rounded-lg border border-destructive/40 bg-destructive/5 p-6 space-y-3">
          <h2 className="text-sm font-semibold text-destructive">Danger Zone</h2>
          {deleteSlot}
        </div>
      )}
    </main>
  )
}

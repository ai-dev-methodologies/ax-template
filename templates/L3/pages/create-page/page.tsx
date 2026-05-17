/*
---
template_id: L3/pages/create-page
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 15 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
  - source_type: internal
    rationale: "Generic create-page skeleton for ax-template L4 composition. Accepts a form slot and cancel href — no domain logic or validation."
dependencies: []
---
*/
import * as React from 'react'

/**
 * CreatePage — generic resource creation form template.
 *
 * Slot props:
 *   - title      (required) page heading
 *   - description (optional) subtitle text
 *   - formSlot   (required) the form content (L2 form block or custom form)
 *   - cancelHref (optional) if provided, renders a Cancel link
 *   - cancelLabel (optional) cancel link label (default: "Cancel")
 *
 * L4 usage:
 *   import CreatePage from 'templates/L3/pages/create-page/page'
 *   export default function NewProductPage() {
 *     return (
 *       <CreatePage
 *         title="New Product"
 *         cancelHref="/products"
 *         formSlot={<ProductCreateForm />}
 *       />
 *     )
 *   }
 */
export interface CreatePageProps {
  /** Page heading */
  title: string
  /** Optional subtitle */
  description?: string
  /** Form content (required) — L2 form block or inline form */
  formSlot: React.ReactNode
  /** If provided, renders a Cancel link next to the form header */
  cancelHref?: string
  /** Label for cancel link (default: "Cancel") */
  cancelLabel?: string
}

export default function CreatePage({
  title,
  description,
  formSlot,
  cancelHref,
  cancelLabel = 'Cancel',
}: CreatePageProps) {
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
    </main>
  )
}

/*
---
template_id: L1/components/label
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: label
    quote: "Renders an accessible label associated with controls."
  - source_type: external
    citation: "WCAG 2.2 SC 1.3.1 Info and Relationships (Level A) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#info-and-relationships"
    quote: "Information, structure, and relationships conveyed through presentation can be programmatically determined or are available in text."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 1.3.1 Info & Relationships — htmlFor must match paired control id"
  - "WCAG 2.2 SC 3.3.2 Labels or Instructions"
  - "Do not use aria-label as substitute for visible <label>"
dependencies: ["@radix-ui/react-label"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#label"
---
*/
import * as React from 'react'
import * as LabelPrimitive from '@radix-ui/react-label'
import { cn } from '../lib/utils'

const Label = React.forwardRef<
  React.ElementRef<typeof LabelPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof LabelPrimitive.Root>
>(({ className, ...props }, ref) => (
  <LabelPrimitive.Root
    ref={ref}
    className={cn(
      'text-[length:--text-sm] font-[number:--weight-medium] leading-none',
      'peer-disabled:cursor-not-allowed peer-disabled:opacity-70',
      className
    )}
    {...props}
  />
))
Label.displayName = LabelPrimitive.Root.displayName

export { Label }

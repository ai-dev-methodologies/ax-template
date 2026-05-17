/*
---
template_id: L1/components/badge
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: badge
    quote: "Displays a badge or a component that looks like a badge."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 1.4.1-use-of-color
    quote: "Color is not used as the only visual means of conveying information."
a11y_criteria:
  - "WCAG 2.2 SC 1.4.3 Contrast — 4.5:1 for normal text"
  - "WCAG 2.2 SC 1.4.1 — Status badges must carry text label, not color alone"
  - "Add aria-label if icon-only badge"
dependencies: ["class-variance-authority"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#badge"
---
*/
import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../lib/utils'

const badgeVariants = cva(
  [
    'inline-flex items-center rounded-[--radius-full]',
    'border px-[--space-2] py-0.5',
    'text-[length:--text-sm] font-[number:--weight-semibold]',
    'transition-colors duration-[--duration-fast]',
    'focus:outline-none focus:ring-2 focus:ring-[--color-focus-ring] focus:ring-offset-2',
  ],
  {
    variants: {
      variant: {
        default:
          'border-transparent bg-[--color-accent] text-[--color-text-inverse] hover:bg-[--color-accent-hover]',
        secondary:
          'border-transparent bg-[--color-surface-subtle] text-[--color-text] hover:bg-[--color-surface-subtle]/80',
        destructive:
          'border-transparent bg-[--color-error] text-[--color-text-inverse] hover:opacity-90',
        outline: 'border-[--color-border] text-[--color-text]',
      },
    },
    defaultVariants: { variant: 'default' },
  }
)

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return (
    <div className={cn(badgeVariants({ variant }), className)} {...props} />
  )
}

export { Badge, badgeVariants }

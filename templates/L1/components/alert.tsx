/*
---
template_id: L1/components/alert
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: alert
    quote: "Displays a callout for user attention."
  - source_type: external
    citation: "WCAG 2.2 SC 1.3.1 Info and Relationships (Level A) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#info-and-relationships"
    quote: "Information, structure, and relationships conveyed through presentation can be programmatically determined or are available in text."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 1.3.1 — use role='alert' for critical alerts, role='status' for informational"
  - "WCAG 2.2 SC 1.4.1 — do not rely on color alone; include icon or label"
  - "Alert content is persistent, not auto-dismissed"
dependencies: []
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#alert"
---
*/
import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../lib/utils'

const alertVariants = cva(
  [
    'relative w-full rounded-[--radius-lg]',
    'border border-[--color-border]',
    'px-[--space-4] py-[--space-3]',
    'text-[length:--text-sm]',
    '[&>svg+div]:translate-y-[-3px]',
    '[&>svg]:absolute [&>svg]:left-[--space-4] [&>svg]:top-[--space-4]',
    '[&>svg]:text-[--color-text]',
    '[&>svg~*]:pl-[--space-7]',
  ],
  {
    variants: {
      variant: {
        default: 'bg-[--color-surface] text-[--color-text]',
        destructive:
          'border-[--color-status-error]/50 text-[--color-status-error] [&>svg]:text-[--color-status-error]',
      },
    },
    defaultVariants: { variant: 'default' },
  }
)

const Alert = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement> & VariantProps<typeof alertVariants>
>(({ className, variant, ...props }, ref) => (
  <div
    ref={ref}
    role="alert"
    className={cn(alertVariants({ variant }), className)}
    {...props}
  />
))
Alert.displayName = 'Alert'

const AlertTitle = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLHeadingElement>
>(({ className, ...props }, ref) => (
  <h5
    ref={ref}
    className={cn('mb-[--space-1] font-[number:--weight-semibold] leading-none tracking-tight', className)}
    {...props}
  />
))
AlertTitle.displayName = 'AlertTitle'

const AlertDescription = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn('text-[length:--text-sm] [&_p]:leading-relaxed', className)}
    {...props}
  />
))
AlertDescription.displayName = 'AlertDescription'

export { Alert, AlertTitle, AlertDescription }

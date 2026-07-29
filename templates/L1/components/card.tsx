/*
---
template_id: L1/components/card
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: card
    quote: "Displays a card with header, content, and footer."
  - source_type: external
    citation: "WCAG 2.2 SC 1.3.1 Info and Relationships (Level A) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#info-and-relationships"
    quote: "Information, structure, and relationships conveyed through presentation can be programmatically determined or are available in text."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 1.3.1 — Use <article> or <section> in L2 block context"
  - "WCAG 2.2 SC 1.4.3 Contrast — text tokens enforce minimum ratio"
dependencies: []
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#card"
---
*/
import * as React from 'react'
import { cn } from '../lib/utils'

const Card = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn(
        'rounded-xl border border-[--color-border]',
        'bg-[--color-surface] text-[--color-text]',
        'shadow-[--shadow-sm]',
        className
      )}
      {...props}
    />
  )
)
Card.displayName = 'Card'

const CardHeader = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn('flex flex-col space-y-[--space-2] p-[--space-6]', className)}
      {...props}
    />
  )
)
CardHeader.displayName = 'CardHeader'

const CardTitle = React.forwardRef<HTMLParagraphElement, React.HTMLAttributes<HTMLHeadingElement>>(
  ({ className, ...props }, ref) => (
    <h3
      ref={ref}
      className={cn(
        'font-[number:--weight-semibold] leading-none tracking-tight',
        className
      )}
      {...props}
    />
  )
)
CardTitle.displayName = 'CardTitle'

const CardDescription = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <p
    ref={ref}
    className={cn('text-[length:--text-sm] text-[--color-text-muted]', className)}
    {...props}
  />
))
CardDescription.displayName = 'CardDescription'

const CardContent = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn('p-[--space-6] pt-0', className)}
      {...props}
    />
  )
)
CardContent.displayName = 'CardContent'

const CardFooter = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cn('flex items-center p-[--space-6] pt-0', className)}
      {...props}
    />
  )
)
CardFooter.displayName = 'CardFooter'

export { Card, CardHeader, CardFooter, CardTitle, CardDescription, CardContent }

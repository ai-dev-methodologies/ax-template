/*
---
template_id: L1/components/separator
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: separator
    quote: "Visually or semantically separates content."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 1.3.1-info-and-relationships
    quote: "Information, structure, and relationships conveyed through presentation can be programmatically determined."
a11y_criteria:
  - "WCAG 2.2 SC 1.3.1 — Radix renders role='separator' or role='none' by context"
  - "Decorative separators must be aria-hidden='true'"
dependencies: ["@radix-ui/react-separator"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#separator"
---
*/
import * as React from 'react'
import * as SeparatorPrimitive from '@radix-ui/react-separator'
import { cn } from '../lib/utils'

const Separator = React.forwardRef<
  React.ElementRef<typeof SeparatorPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof SeparatorPrimitive.Root>
>(({ className, orientation = 'horizontal', decorative = true, ...props }, ref) => (
  <SeparatorPrimitive.Root
    ref={ref}
    decorative={decorative}
    orientation={orientation}
    className={cn(
      'shrink-0 bg-[--color-border]',
      orientation === 'horizontal' ? 'h-[1px] w-full' : 'h-full w-[1px]',
      className
    )}
    {...props}
  />
))
Separator.displayName = SeparatorPrimitive.Root.displayName

export { Separator }

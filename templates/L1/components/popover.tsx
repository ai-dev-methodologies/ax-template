/*
---
template_id: L1/components/popover
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: popover
    quote: "Displays rich content in a portal, triggered by a button."
  - source_type: external
    citation: "WCAG 2.2 SC 2.4.3 Focus Order (Level A) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#focus-order"
    quote: "If a web page can be navigated sequentially and the navigation sequences affect meaning or operation, focusable components receive focus in an order that preserves meaning and operability."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 4.1.2 — trigger button has aria-expanded via Radix"
  - "WCAG 2.2 SC 2.4.3 Focus Order"
  - "WCAG 2.2 SC 2.4.11 Focus Appearance"
  - "Close on Escape and on focus leaving popover"
dependencies: ["@radix-ui/react-popover"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#popover"
---
*/
import * as React from 'react'
import * as PopoverPrimitive from '@radix-ui/react-popover'
import { cn } from '../lib/utils'

const Popover = PopoverPrimitive.Root
const PopoverTrigger = PopoverPrimitive.Trigger
const PopoverAnchor = PopoverPrimitive.Anchor

const PopoverContent = React.forwardRef<
  React.ElementRef<typeof PopoverPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof PopoverPrimitive.Content>
>(({ className, align = 'center', sideOffset = 4, ...props }, ref) => (
  <PopoverPrimitive.Portal>
    <PopoverPrimitive.Content
      ref={ref}
      align={align}
      sideOffset={sideOffset}
      className={cn(
        'z-[--z-tooltip] w-72 rounded-[--radius-md]',
        'border border-[--color-border] bg-[--color-surface]',
        'p-[--space-4] text-[--color-text]',
        'shadow-[--shadow-md] outline-none',
        'data-[state=open]:animate-in data-[state=closed]:animate-out',
        'data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0',
        'data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95',
        'data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2',
        'data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2',
        className
      )}
      {...props}
    />
  </PopoverPrimitive.Portal>
))
PopoverContent.displayName = PopoverPrimitive.Content.displayName

export { Popover, PopoverTrigger, PopoverContent, PopoverAnchor }

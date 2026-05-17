/*
---
template_id: L1/components/hover-card
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: hover-card
    quote: "For sighted users to preview content available behind a link."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 1.4.13-content-on-hover-or-focus
    quote: "Where receiving and then removing pointer hover or keyboard focus triggers additional content to become visible and then hidden, the following are true: Dismissible, Hoverable, Persistent."
a11y_criteria:
  - "WCAG 2.2 SC 1.4.13 — content dismissible by Escape"
  - "WCAG 2.2 SC 2.5.8 — adequate target size"
  - "All interactive content inside must be keyboard-reachable"
dependencies: ["@radix-ui/react-hover-card"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#hover-card"
---
*/
import * as React from 'react'
import * as HoverCardPrimitive from '@radix-ui/react-hover-card'
import { cn } from '../lib/utils'

const HoverCard = HoverCardPrimitive.Root
const HoverCardTrigger = HoverCardPrimitive.Trigger

const HoverCardContent = React.forwardRef<
  React.ElementRef<typeof HoverCardPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof HoverCardPrimitive.Content>
>(({ className, align = 'center', sideOffset = 4, ...props }, ref) => (
  <HoverCardPrimitive.Content
    ref={ref}
    align={align}
    sideOffset={sideOffset}
    className={cn(
      'z-[--z-tooltip] w-64 rounded-[--radius-md]',
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
))
HoverCardContent.displayName = HoverCardPrimitive.Content.displayName

export { HoverCard, HoverCardTrigger, HoverCardContent }

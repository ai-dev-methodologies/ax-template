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
  - source_type: external
    citation: "WCAG 2.2 SC 1.4.13 Content on Hover or Focus (Level AA) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#content-on-hover-or-focus"
    quote: "Where receiving and then removing pointer hover or keyboard focus triggers additional content to become visible and then hidden, the following are true: Dismissible: A mechanism is available to dismiss the additional content without moving pointer hover or keyboard focus, unless the additional content communicates an input error or does not obscure or replace other content; Hoverable: If pointer hover can trigger the additional content, then the pointer can be moved over the additional content without the additional content disappearing; Persistent: The additional content remains visible until the hover or focus trigger is removed, the user dismisses it, or its information is no longer valid. Exception: The visual presentation of the additional content is controlled by the user agent and is not modified by the author."
    quoted_at: "2026-07-29"
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

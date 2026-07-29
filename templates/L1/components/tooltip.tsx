/*
---
template_id: L1/components/tooltip
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: tooltip
    quote: "A popup that displays information related to an element when the element receives keyboard focus or the mouse hovers over it."
  - source_type: external
    citation: "WCAG 2.2 SC 1.4.13 Content on Hover or Focus (Level AA) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#content-on-hover-or-focus"
    quote: "Where receiving and then removing pointer hover or keyboard focus triggers additional content to become visible and then hidden, the following are true: Dismissible: A mechanism is available to dismiss the additional content without moving pointer hover or keyboard focus, unless the additional content communicates an input error or does not obscure or replace other content; Hoverable: If pointer hover can trigger the additional content, then the pointer can be moved over the additional content without the additional content disappearing; Persistent: The additional content remains visible until the hover or focus trigger is removed, the user dismisses it, or its information is no longer valid. Exception: The visual presentation of the additional content is controlled by the user agent and is not modified by the author."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 1.4.13 — dismissible, hoverable, persistent"
  - "Must appear on keyboard focus, not hover only"
  - "role='tooltip' + aria-describedby"
  - "Tooltip content must not be the only source of important info"
dependencies: ["@radix-ui/react-tooltip"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#tooltip"
---
*/
import * as React from 'react'
import * as TooltipPrimitive from '@radix-ui/react-tooltip'
import { cn } from '../lib/utils'

const TooltipProvider = TooltipPrimitive.Provider
const Tooltip = TooltipPrimitive.Root
const TooltipTrigger = TooltipPrimitive.Trigger

const TooltipContent = React.forwardRef<
  React.ElementRef<typeof TooltipPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof TooltipPrimitive.Content>
>(({ className, sideOffset = 4, ...props }, ref) => (
  <TooltipPrimitive.Portal>
    <TooltipPrimitive.Content
      ref={ref}
      sideOffset={sideOffset}
      className={cn(
        'z-[--z-tooltip] overflow-hidden rounded-[--radius-md]',
        'border border-[--color-border] bg-[--color-surface-inverse]',
        'px-[--space-3] py-[--space-2]',
        'text-[--color-text-inverse] text-[length:--text-sm]',
        'shadow-[--shadow-md]',
        'animate-in fade-in-0 zoom-in-95',
        'data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95',
        'data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2',
        'data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2',
        className
      )}
      {...props}
    />
  </TooltipPrimitive.Portal>
))
TooltipContent.displayName = TooltipPrimitive.Content.displayName

export { Tooltip, TooltipTrigger, TooltipContent, TooltipProvider }

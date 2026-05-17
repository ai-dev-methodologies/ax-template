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
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 1.4.13-content-on-hover-or-focus
    quote: "Where receiving and then removing pointer hover or keyboard focus triggers additional content to become visible and then hidden, the following are true: Dismissible, Hoverable, Persistent."
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

/*
---
template_id: L1/components/tabs
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: tabs
    quote: "A set of layered sections of content—known as tab panels—that are displayed one at a time."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 4.1.2-name-role-value
    quote: "For all user interface components, the name and role can be programmatically determined."
a11y_criteria:
  - "WCAG 2.2 SC 4.1.2 — role='tablist', role='tab', role='tabpanel' via Radix"
  - "WCAG 2.2 SC 2.4.3 Focus Order — Radix manages focus within tablist"
  - "Do not apply outline: none to tablist without replacement focus indicator"
dependencies: ["@radix-ui/react-tabs"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#tabs"
---
*/
import * as React from 'react'
import * as TabsPrimitive from '@radix-ui/react-tabs'
import { cn } from '../lib/utils'

const Tabs = TabsPrimitive.Root

const TabsList = React.forwardRef<
  React.ElementRef<typeof TabsPrimitive.List>,
  React.ComponentPropsWithoutRef<typeof TabsPrimitive.List>
>(({ className, ...props }, ref) => (
  <TabsPrimitive.List
    ref={ref}
    className={cn(
      'inline-flex h-9 items-center justify-center',
      'rounded-[--radius-lg] bg-[--color-surface-subtle] p-1',
      'text-[--color-text-muted]',
      className
    )}
    {...props}
  />
))
TabsList.displayName = TabsPrimitive.List.displayName

const TabsTrigger = React.forwardRef<
  React.ElementRef<typeof TabsPrimitive.Trigger>,
  React.ComponentPropsWithoutRef<typeof TabsPrimitive.Trigger>
>(({ className, ...props }, ref) => (
  <TabsPrimitive.Trigger
    ref={ref}
    className={cn(
      'inline-flex items-center justify-center whitespace-nowrap',
      'rounded-[--radius-md] px-[--space-3] py-1',
      'text-[length:--text-sm] font-[number:--weight-medium]',
      'ring-offset-[--color-surface]',
      'transition-all duration-[--duration-fast]',
      'focus-visible:outline-none focus-visible:ring-2',
      'focus-visible:ring-[--color-focus-ring] focus-visible:ring-offset-2',
      'disabled:pointer-events-none disabled:opacity-50',
      'data-[state=active]:bg-[--color-surface]',
      'data-[state=active]:text-[--color-text]',
      'data-[state=active]:shadow-[--shadow-sm]',
      className
    )}
    {...props}
  />
))
TabsTrigger.displayName = TabsPrimitive.Trigger.displayName

const TabsContent = React.forwardRef<
  React.ElementRef<typeof TabsPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof TabsPrimitive.Content>
>(({ className, ...props }, ref) => (
  <TabsPrimitive.Content
    ref={ref}
    className={cn(
      'mt-[--space-2] ring-offset-[--color-surface]',
      'focus-visible:outline-none focus-visible:ring-2',
      'focus-visible:ring-[--color-focus-ring] focus-visible:ring-offset-2',
      className
    )}
    {...props}
  />
))
TabsContent.displayName = TabsPrimitive.Content.displayName

export { Tabs, TabsList, TabsTrigger, TabsContent }

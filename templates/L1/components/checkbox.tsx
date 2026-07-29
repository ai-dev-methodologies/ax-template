/*
---
template_id: L1/components/checkbox
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: checkbox
    quote: "A control that allows the user to toggle between checked and not checked."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: "SC 1.4.11 — Non-text Contrast — Level AA"
    quote: "The visual presentation of the following have a contrast ratio of at least 3:1 against adjacent color(s): User Interface Components: Visual information required to identify user interface components and states"
a11y_criteria:
  - "WCAG 2.2 SC 4.1.2 Name/Role/Value — aria-checked state managed by Radix"
  - "WCAG 2.2 SC 1.4.11 Non-text Contrast — border uses --color-border (≥3:1)"
  - "Mixed/indeterminate state: aria-checked='mixed', not CSS-only"
dependencies: ["@radix-ui/react-checkbox"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#checkbox"
---
*/
import * as React from 'react'
import * as CheckboxPrimitive from '@radix-ui/react-checkbox'
import { cn } from '../lib/utils'

const Checkbox = React.forwardRef<
  React.ElementRef<typeof CheckboxPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof CheckboxPrimitive.Root>
>(({ className, ...props }, ref) => (
  <CheckboxPrimitive.Root
    ref={ref}
    className={cn(
      'peer h-4 w-4 shrink-0 rounded-[--radius-sm]',
      'border border-[--color-border] shadow-[--shadow-sm]',
      'focus-visible:outline-none focus-visible:ring-2',
      'focus-visible:ring-[--color-focus-ring] focus-visible:ring-offset-2',
      'disabled:cursor-not-allowed disabled:opacity-50',
      'data-[state=checked]:bg-[--color-accent]',
      'data-[state=checked]:text-[--color-text-inverse]',
      className
    )}
    {...props}
  >
    <CheckboxPrimitive.Indicator
      className={cn('flex items-center justify-center text-current')}
    >
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>
    </CheckboxPrimitive.Indicator>
  </CheckboxPrimitive.Root>
))
Checkbox.displayName = CheckboxPrimitive.Root.displayName

export { Checkbox }

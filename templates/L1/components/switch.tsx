/*
---
template_id: L1/components/switch
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: switch
    quote: "A control that allows the user to toggle between checked and not checked."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 4.1.2-name-role-value
    quote: "For all user interface components, the name and role can be programmatically determined."
a11y_criteria:
  - "WCAG 2.2 SC 4.1.2 — role='switch' + aria-checked managed by Radix"
  - "WCAG 2.2 SC 2.5.8 — 44×44 touch target on mobile (coarse pointer)"
  - "WCAG 2.2 SC 1.4.1 — ON/OFF state must not rely on color alone; add text label"
dependencies: ["@radix-ui/react-switch"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#switch"
---
*/
import * as React from 'react'
import * as SwitchPrimitive from '@radix-ui/react-switch'
import { cn } from '../lib/utils'

const Switch = React.forwardRef<
  React.ElementRef<typeof SwitchPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof SwitchPrimitive.Root>
>(({ className, ...props }, ref) => (
  <SwitchPrimitive.Root
    className={cn(
      'peer inline-flex h-5 w-9 shrink-0 cursor-pointer items-center',
      'rounded-full border-2 border-transparent',
      'transition-colors duration-[--duration-fast]',
      'focus-visible:outline-none focus-visible:ring-2',
      'focus-visible:ring-[--color-focus-ring] focus-visible:ring-offset-2',
      'disabled:cursor-not-allowed disabled:opacity-50',
      'data-[state=checked]:bg-[--color-accent]',
      'data-[state=unchecked]:bg-[--color-border]',
      className
    )}
    {...props}
    ref={ref}
  >
    <SwitchPrimitive.Thumb
      className={cn(
        'pointer-events-none block h-4 w-4 rounded-full',
        'bg-[--color-surface] shadow-[--shadow-md]',
        'ring-0 transition-transform duration-[--duration-fast]',
        'data-[state=checked]:translate-x-4',
        'data-[state=unchecked]:translate-x-0'
      )}
    />
  </SwitchPrimitive.Root>
))
Switch.displayName = SwitchPrimitive.Root.displayName

export { Switch }

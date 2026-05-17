/*
---
template_id: L1/components/radio-group
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: radio-group
    quote: "A set of checkable buttons—known as radio buttons—where no more than one of the buttons can be checked at a time."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 4.1.2-name-role-value
    quote: "For all user interface components, the name and role can be programmatically determined."
a11y_criteria:
  - "WCAG 2.2 SC 4.1.2 Name/Role/Value — Radix renders role='radiogroup'"
  - "WCAG 2.2 SC 1.3.1 — Wrap in <fieldset> + <legend> at L2 block level"
dependencies: ["@radix-ui/react-radio-group"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#radio-group"
---
*/
import * as React from 'react'
import * as RadioGroupPrimitive from '@radix-ui/react-radio-group'
import { cn } from '../lib/utils'

const RadioGroup = React.forwardRef<
  React.ElementRef<typeof RadioGroupPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof RadioGroupPrimitive.Root>
>(({ className, ...props }, ref) => {
  return (
    <RadioGroupPrimitive.Root
      className={cn('grid gap-[--space-2]', className)}
      {...props}
      ref={ref}
    />
  )
})
RadioGroup.displayName = RadioGroupPrimitive.Root.displayName

const RadioGroupItem = React.forwardRef<
  React.ElementRef<typeof RadioGroupPrimitive.Item>,
  React.ComponentPropsWithoutRef<typeof RadioGroupPrimitive.Item>
>(({ className, ...props }, ref) => {
  return (
    <RadioGroupPrimitive.Item
      ref={ref}
      className={cn(
        'aspect-square h-4 w-4 rounded-full',
        'border border-[--color-border] text-[--color-accent]',
        'shadow-[--shadow-sm]',
        'focus:outline-none focus-visible:ring-2',
        'focus-visible:ring-[--color-focus-ring] focus-visible:ring-offset-2',
        'disabled:cursor-not-allowed disabled:opacity-50',
        className
      )}
      {...props}
    >
      <RadioGroupPrimitive.Indicator className="flex items-center justify-center">
        <svg xmlns="http://www.w3.org/2000/svg" width="9" height="9" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><circle cx="12" cy="12" r="12"/></svg>
      </RadioGroupPrimitive.Indicator>
    </RadioGroupPrimitive.Item>
  )
})
RadioGroupItem.displayName = RadioGroupPrimitive.Item.displayName

export { RadioGroup, RadioGroupItem }

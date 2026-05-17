/*
---
template_id: L1/components/button
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: button
    quote: "Displays a button or a component that looks like a button."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 2.5.8-target-size-minimum
    quote: "The size of the target for pointer inputs is at least 24 by 24 CSS pixels."
a11y_criteria:
  - "WCAG 2.2 SC 2.5.8 Target Size (Minimum) — min-h-6 min-w-6 enforced"
  - "WCAG 2.2 SC 4.1.2 Name/Role/Value — aria-label required when icon-only"
  - "WCAG 2.2 SC 2.4.11 Focus Appearance — focus ring uses --color-focus-ring"
dependencies: ["@radix-ui/react-slot", "class-variance-authority"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#button"
---
*/
import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../lib/utils'

const buttonVariants = cva(
  [
    'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-[--radius-md]',
    'text-[length:--text-base] font-[number:--weight-medium]',
    'transition-colors duration-[--duration-fast]',
    'focus-visible:outline-none focus-visible:ring-2',
    'focus-visible:ring-[--color-focus-ring] focus-visible:ring-offset-2',
    'disabled:pointer-events-none disabled:opacity-50',
    '[&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0',
    // WCAG 2.2 SC 2.5.8: minimum 24×24 px target
    'min-h-6 min-w-6',
  ],
  {
    variants: {
      variant: {
        default:
          'bg-[--color-accent] text-[--color-text-inverse] hover:bg-[--color-accent-hover]',
        destructive:
          'bg-[--color-error] text-[--color-text-inverse] hover:opacity-90',
        outline:
          'border border-[--color-border] bg-transparent hover:bg-[--color-surface-subtle]',
        ghost: 'hover:bg-[--color-surface-subtle]',
        link: 'text-[--color-accent] underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-9 px-[--space-4] py-[--space-2]',
        sm: 'h-8 px-[--space-3] text-[length:--text-sm]',
        lg: 'h-11 px-[--space-6]',
        icon: 'h-9 w-9',
      },
    },
    defaultVariants: { variant: 'default', size: 'default' },
  }
)

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : 'button'
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    )
  }
)
Button.displayName = 'Button'

export { Button, buttonVariants }

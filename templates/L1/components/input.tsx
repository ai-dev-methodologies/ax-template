/*
---
template_id: L1/components/input
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: input
    quote: "Displays a form input field or a component that looks like an input field."
  - source_type: external
    citation: "WCAG 2.2 SC 3.3.2 Labels or Instructions (Level A) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#labels-or-instructions"
    quote: "Labels or instructions are provided when content requires user input."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 1.3.5 Identify Input Purpose — autocomplete attribute required"
  - "WCAG 2.2 SC 3.3.2 Labels or Instructions — always pair with <Label>"
  - "Never use placeholder as the only label"
dependencies: []
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#input"
---
*/
import * as React from 'react'
import { cn } from '../lib/utils'

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, ...props }, ref) => {
    return (
      <input
        type={type}
        className={cn(
          'flex h-9 w-full rounded-[--radius-md] border border-[--color-border]',
          'bg-transparent px-[--space-3] py-1',
          'text-[length:--text-base] placeholder:text-[--color-text-placeholder]',
          'shadow-[--shadow-sm]',
          'transition-colors duration-[--duration-fast]',
          'focus-visible:outline-none focus-visible:ring-2',
          'focus-visible:ring-[--color-focus-ring] focus-visible:ring-offset-2',
          'disabled:cursor-not-allowed disabled:opacity-50',
          className
        )}
        ref={ref}
        {...props}
      />
    )
  }
)
Input.displayName = 'Input'

export { Input }

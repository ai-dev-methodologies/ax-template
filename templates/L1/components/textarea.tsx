/*
---
template_id: L1/components/textarea
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: textarea
    quote: "Displays a form textarea or a component that looks like a textarea."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 3.3.2-labels-or-instructions
    quote: "Labels or instructions are provided when content requires user input."
a11y_criteria:
  - "WCAG 2.2 SC 3.3.2 Labels or Instructions — always pair with <Label>"
  - "WCAG 2.2 SC 1.3.5 Identify Input Purpose"
dependencies: []
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#textarea"
---
*/
import * as React from 'react'
import { cn } from '../lib/utils'

export interface TextareaProps
  extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {}

const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, ...props }, ref) => {
    return (
      <textarea
        className={cn(
          'flex min-h-[60px] w-full rounded-[--radius-md] border border-[--color-border]',
          'bg-transparent px-[--space-3] py-[--space-2]',
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
Textarea.displayName = 'Textarea'

export { Textarea }

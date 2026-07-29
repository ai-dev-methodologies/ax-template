/*
---
template_id: L1/components/sonner
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: sonner
    quote: "An opinionated toast component for React."
  - source_type: upstream_id
    upstream_id: wcag-22-techniques-2026-05
    section: "Success Criterion (SC)"
    quote: "In content implemented using markup languages, status messages can be programmatically determined through role or properties such that they can be presented to the user by assistive technologies without receiving focus."
a11y_criteria:
  - "WCAG 2.2 SC 4.1.3 — role='status' or role='alert' on toast notifications"
  - "Toast auto-dismiss duration must be ≥5s or user-controllable"
  - "Not the only mechanism to convey critical information"
dependencies: ["sonner"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#sonner"
---
*/
import { Toaster as Sonner } from 'sonner'

type ToasterProps = React.ComponentProps<typeof Sonner>

const Toaster = ({ ...props }: ToasterProps) => (
  <Sonner
    className="toaster group"
    toastOptions={{
      classNames: {
        toast:
          'group toast group-[.toaster]:bg-[--color-surface] group-[.toaster]:text-[--color-text] group-[.toaster]:border-[--color-border] group-[.toaster]:shadow-[--shadow-lg]',
        description: 'group-[.toast]:text-[--color-text-muted]',
        actionButton:
          'group-[.toast]:bg-[--color-accent] group-[.toast]:text-[--color-text-inverse]',
        cancelButton:
          'group-[.toast]:bg-[--color-surface-subtle] group-[.toast]:text-[--color-text-muted]',
      },
    }}
    {...props}
  />
)

export { Toaster }

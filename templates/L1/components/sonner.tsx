/*
---
template_id: L1/components/sonner
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: sonner
    quote: "A succinct message that is displayed temporarily."
  - source_type: upstream_id
    upstream_id: wcag-22-techniques-2026-05
    section: "Success Criterion (SC)"
    quote: "In content implemented using markup languages, status messages can be programmatically determined through role or properties such that they can be presented to the user by assistive technologies without receiving focus."
  # P2-73 B-class disposition (2026-08-01). The anchor above resolves PRACTICES-FIRST
  # (evidence_quote_spotcheck_guard.resolve_snapshot_any_catalog tries practices/ then
  # practices-react/), and the two catalogs register the SAME id against DIFFERENT pages:
  # practices/upstream/wcag-22-techniques-2026-05 is the SC 4.1.3 Understanding page (which
  # does carry this sentence verbatim), practices-react/upstream/ is the Techniques INDEX
  # (which does not). The citation is therefore true only by resolution ORDER. This second
  # anchor states the same normative sentence against the WCAG 2.2 RECOMMENDATION itself, so
  # the claim no longer depends on which catalog wins.
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: "Success Criterion 4.1.3 Status Messages"
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

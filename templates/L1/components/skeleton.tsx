/*
---
template_id: L1/components/skeleton
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: skeleton
    quote: "Use to show a placeholder while content is loading."
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
  - "WCAG 2.2 SC 4.1.3 Status Messages — parent region needs aria-busy='true' + aria-label='Loading'"
  - "Individual Skeleton divs are aria-hidden='true'"
dependencies: []
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#skeleton"
---
*/
import * as React from 'react'
import { cn } from '../lib/utils'

function Skeleton({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      aria-hidden="true"
      className={cn(
        'animate-pulse rounded-[--radius-md] bg-[--color-surface-subtle]',
        className
      )}
      {...props}
    />
  )
}

export { Skeleton }

/*
---
template_id: L2/blocks/status-badge
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 status pill codified from a community status-badge pattern. Normalized to ax invariants: semantic design tokens (--ax-status-*, zero raw hex), role=status + aria-label, typed StatusKind string-literal union (ax: no enum), one parameterized component. Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md (spec REACT-PRACTICES-UX-001)."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
// Colors come from design tokens, never hardcoded hex. The consuming theme defines
// --ax-status-<token>-fg / --ax-status-<token>-bg (light + dark + brand); one swap re-skins every badge.
import type { ComponentType, SVGProps } from 'react'
import {
  CircleCheck, CircleDashed, CircleX, Clock5, ScanSearch, TriangleAlert,
} from 'lucide-react'

export type StatusKind =
  | 'pending' | 'failed' | 'success' | 'in_progress' | 'in_review' | 'expired' | 'submitted'

interface StatusSpec {
  label: string
  Icon: ComponentType<SVGProps<SVGSVGElement>>
  /** semantic token name — resolved by the consuming theme, never a raw hex */
  token: string
}

// Status -> (label, icon, design-token). The token is the ONLY color reference;
// the theme maps `--ax-status-<token>-fg` / `--ax-status-<token>-bg` to actual values.
const STATUS: Record<StatusKind, StatusSpec> = {
  pending:     { label: 'Pending',     Icon: TriangleAlert, token: 'warning' },
  failed:      { label: 'Failed',      Icon: CircleX,       token: 'danger' },
  success:     { label: 'Success',     Icon: CircleCheck,   token: 'success' },
  in_progress: { label: 'In progress', Icon: CircleDashed,  token: 'info' },
  in_review:   { label: 'In review',   Icon: ScanSearch,    token: 'attention' },
  expired:     { label: 'Expired',     Icon: Clock5,        token: 'neutral' },
  submitted:   { label: 'Submitted',   Icon: Clock5,        token: 'accent' },
}

export interface StatusBadgeProps {
  status: StatusKind
  /** override the default label (still announced to assistive tech) */
  label?: string
  className?: string
}

export default function StatusBadge({ status, label, className }: StatusBadgeProps) {
  const spec = STATUS[status]
  const text = label ?? spec.label
  return (
    <span
      role="status"
      aria-label={text}
      data-status={status}
      className={['ax-status-badge', className].filter(Boolean).join(' ')}
      style={{
        color: `var(--ax-status-${spec.token}-fg)`,
        background: `var(--ax-status-${spec.token}-bg)`,
      }}
    >
      <spec.Icon className="ax-status-badge__icon" strokeWidth={3} aria-hidden="true" />
      {text}
    </span>
  )
}

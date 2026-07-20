/*
---
template_id: L4/audit-log/app/(audit-log)/[id]/audit-log-detail-view
layer: L4
domain: audit-log
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (audit-log)/[id]/page.tsx (S2.AUDIT-PII.XB
      render-testability closure): the detail page's data-fetch orchestration (useQuery/
      useRouter) is a hard dependency-resolution boundary for a vitest test that imports this
      file directly from outside frontend/ (see frontend/tests/audit-log-redaction-render.
      vitest.tsx for the reproduction). Splitting the props->JSX render surface into its own
      file with zero external-npm imports makes the redacted-actorIp render path unit-testable
      without touching shared vitest config."
---
*/
import * as React from 'react'

// ─── types ───────────────────────────────────────────────────────────────────

export interface AuditLogDetail {
  id: string
  actorId: string
  actorIp: string | null
  action: string
  resourceType: string
  resourceId: string | null
  outcome: 'SUCCESS' | 'FAILURE'
  timestamp: string
  metadata: Record<string, unknown> | null
  correlationId: string | null
  userAgent: string | null
}

// ─── sub-component: labeled field row ─────────────────────────────────────────

function FieldRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1 py-2 border-b last:border-b-0">
      <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
        {label}
      </span>
      <span className="text-sm break-all">{value ?? <span className="italic text-muted-foreground">—</span>}</span>
    </div>
  )
}

// ─── component ────────────────────────────────────────────────────────────────

/**
 * AuditLogDetailView — pure presentational render of a single audit log entry (AUDIT-FE-004).
 *
 * Displays: id, actorId, actorIp, action, resourceType, resourceId, outcome, timestamp,
 * correlationId, userAgent, metadata (collapsible JSON block).
 *
 * Deliberately has ZERO data-fetching / routing dependencies (no useQuery, no useRouter) —
 * the caller (`(audit-log)/[id]/page.tsx`) owns loading/error/not-found orchestration and
 * passes the resolved `data` in. This keeps the component a plain props -> JSX function,
 * which is what makes it renderable in a unit test without a QueryClientProvider or a
 * next/navigation mock.
 *
 * `actorIp` is rendered EXACTLY as received — no client-side masking. The redaction
 * guarantee (AUDIT-PII-001) is a backend concern: `AuditLogPiiRedactor` masks the IP before
 * it is ever persisted, so whatever this component renders is what the API actually returned.
 */
export default function AuditLogDetailView({ data }: { data: AuditLogDetail }) {
  return (
    <>
      <div className="rounded-lg border bg-card p-4">
        <FieldRow label="ID" value={data.id} />
        <FieldRow label="Timestamp" value={new Date(data.timestamp).toLocaleString()} />
        <FieldRow label="Actor" value={data.actorId} />
        <FieldRow label="Actor IP" value={data.actorIp} />
        <FieldRow label="Action" value={data.action} />
        <FieldRow label="Resource Type" value={data.resourceType} />
        <FieldRow label="Resource ID" value={data.resourceId} />
        <FieldRow
          label="Outcome"
          value={
            <span
              style={{
                color: data.outcome === 'SUCCESS'
                  ? 'var(--color-success, green)'
                  : 'var(--color-error, red)',
                fontWeight: 600,
              }}
            >
              {data.outcome}
            </span>
          }
        />
        <FieldRow label="Correlation ID" value={data.correlationId} />
        <FieldRow label="User Agent" value={data.userAgent} />
      </div>

      {data.metadata && (
        <div className="rounded-lg border bg-card p-4">
          <h2 className="text-sm font-medium mb-2">Metadata</h2>
          <pre className="text-xs overflow-auto max-h-64 bg-muted p-2 rounded">
            {JSON.stringify(data.metadata, null, 2)}
          </pre>
        </div>
      )}
    </>
  )
}

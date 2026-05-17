/*
---
template_id: L4/audit-log/app/(audit-log)/[id]/page
layer: L4
domain: audit-log
domain_mode: full_trio
backend_operation_id: getAuditLog
evidence:
  - source_type: internal
    rationale: "L4 audit-log vertical — DETAIL page for a single audit log entry. Renders all fields including metadata JSON block, correlationId, and userAgent."
  - source_type: external
    citation: "TanStack Query v5 — useQuery for single resource fetch"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
'use client'

import * as React from 'react'
import { useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'

// ─── types ───────────────────────────────────────────────────────────────────

interface AuditLogDetail {
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

interface AuditLogDetailPageProps {
  params: { id: string }
}

// ─── API ──────────────────────────────────────────────────────────────────────

async function fetchAuditLog(id: string): Promise<AuditLogDetail> {
  const res = await fetch(`/api/audit-logs/${encodeURIComponent(id)}`)
  if (res.status === 404) throw new Error('not_found')
  if (!res.ok) throw new Error(`getAuditLog failed: ${res.status}`)
  return res.json()
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
 * AuditLogDetailPage — full detail view for a single audit log entry (AUDIT-FE-004).
 *
 * Displays: id, actorId, actorIp, action, resourceType, resourceId, outcome,
 * timestamp, correlationId, userAgent, metadata (collapsible JSON block).
 *
 * Returns 404 inline state when the entry is not found.
 *
 * Fork instructions:
 *   1. Add breadcrumb: Audit Log > {id slice}.
 *   2. Add a "Copy ID" button for correlationId for easier trace linkage.
 *   3. Render metadata as a formatted JSON diff viewer for better readability.
 */
export default function AuditLogDetailPage({ params }: AuditLogDetailPageProps) {
  const router = useRouter()
  const { id } = params

  const { data, isLoading, error } = useQuery({
    queryKey: ['audit-log', id],
    queryFn: () => fetchAuditLog(id),
    retry: (failureCount, err) => {
      // Do not retry on 404
      if ((err as Error).message === 'not_found') return false
      return failureCount < 2
    },
  })

  if (isLoading) {
    return <div className="p-4 text-muted-foreground">Loading…</div>
  }

  if (error) {
    const isNotFound = (error as Error).message === 'not_found'
    return (
      <div className="p-4">
        <p className="text-destructive mb-2">
          {isNotFound ? 'Audit log entry not found.' : 'Failed to load audit log entry.'}
        </p>
        <button onClick={() => router.back()} className="text-sm underline">
          ← Back to list
        </button>
      </div>
    )
  }

  if (!data) return null

  return (
    <div className="flex flex-col gap-4 max-w-2xl">
      <div className="flex items-center gap-2">
        <button onClick={() => router.back()} className="text-sm text-muted-foreground underline">
          ← Back to list
        </button>
      </div>

      <h1 className="text-xl font-semibold">Audit Log Entry</h1>

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
    </div>
  )
}

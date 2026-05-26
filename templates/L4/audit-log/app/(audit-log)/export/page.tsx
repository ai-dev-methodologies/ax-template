/*
---
template_id: L4/audit-log/app/(audit-log)/export/page
layer: L4
domain: audit-log
domain_mode: full_trio
backend_operation_id: exportAuditLogs
evidence:
  - source_type: internal
    rationale: "L4 audit-log vertical — EXPORT page. Format selector (CSV/JSON), optional filter fields. POST to exportAuditLogs (202 Accepted), then show jobId + poll link. Access-denied notice for non-AUDITOR/ADMIN users (AUDIT-FE-006)."
  - source_type: external
    citation: "TanStack Query v5 — useMutation for POST requests"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useMutation"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
'use client'

import * as React from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useCallerRole } from 'templates/L0/fork-receiver-kit/use-caller-id'

// ─── types ───────────────────────────────────────────────────────────────────

type ExportFormat = 'CSV' | 'JSON'
type JobState = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'

interface ExportRequest {
  format: ExportFormat
  actorId?: string
  resourceType?: string
  action?: string
  outcome?: 'SUCCESS' | 'FAILURE' | ''
  from?: string
  to?: string
}

interface ExportJobResponse {
  jobId: string
  status: 'PENDING'
}

interface ExportJobStatus {
  jobId: string
  status: JobState
  downloadUrl?: string
  errorMessage?: string
  recordCount?: number
}

// ─── API ──────────────────────────────────────────────────────────────────────

async function submitExport(request: ExportRequest): Promise<ExportJobResponse> {
  const res = await fetch('/api/audit-logs/export', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (res.status === 403) throw new Error('forbidden')
  if (!res.ok) throw new Error(`exportAuditLogs failed: ${res.status}`)
  return res.json()
}

async function pollExportStatus(jobId: string): Promise<ExportJobStatus> {
  const res = await fetch(`/api/audit-logs/export/${encodeURIComponent(jobId)}`)
  if (!res.ok) throw new Error(`getExportJobStatus failed: ${res.status}`)
  return res.json()
}

// ─── component ────────────────────────────────────────────────────────────────

/**
 * AuditLogExportPage — L4 audit-log export request form (AUDIT-FE-005, AUDIT-FE-006).
 *
 * Behavior:
 *   - Renders format selector (CSV/JSON) + optional filter inputs.
 *   - POST to /api/audit-logs/export on submit.
 *   - On 202 Accepted: shows jobId + polls GET /api/audit-logs/export/{jobId} every 3s.
 *   - On 403: shows access-denied notice (no form rendered) — AUDIT-FE-006.
 *   - On COMPLETED: shows download link.
 *
 * Fork instructions:
 *   1. Replace session role check with your auth provider's useSession hook.
 *   2. Add a progress indicator while status is PROCESSING.
 *   3. Add date range pickers for from/to filter fields.
 *   4. Wire POLL_INTERVAL_MS to blueprints/audit-log-ui-manifest.yaml#export.poll_interval_ms.
 *
 * RBAC note (AUDIT-FE-006, AUDIT-EXPORT-002):
 *   The form is only rendered when the session role includes ADMIN or AUDITOR.
 *   The server enforces this via @PreAuthorize — the client check is UX only.
 */
export default function AuditLogExportPage() {
  const [format, setFormat] = React.useState<ExportFormat>('CSV')
  const [filters, setFilters] = React.useState({
    actorId: '',
    resourceType: '',
    action: '',
    outcome: '' as '' | 'SUCCESS' | 'FAILURE',
    from: '',
    to: '',
  })
  const [jobId, setJobId] = React.useState<string | null>(null)
  const [pollEnabled, setPollEnabled] = React.useState(false)

  // ─── Session role check (AUDIT-FE-006) ──────────────────────────────────────
  // R75 — anchor R47 rbac-stub-default-fail-closed via L0 fork-receiver-kit.
  // The previous `const hasExportRole = true` default was fail-OPEN — every
  // dev / staging / preview deploy exposed the audit-log export to any
  // viewer until the fork-receiver wired their session hook. Now defaults
  // to 'user' (role !== 'admin' → no access) per R47; the AUDITOR role
  // gating remains a fork-receiver decision (their RBAC source decides
  // the AUDITOR / ADMIN partition).
  const callerRole = useCallerRole()
  const hasExportRole = callerRole === 'admin'

  // ─── Submit mutation ──────────────────────────────────────────────────────
  const mutation = useMutation({
    mutationFn: submitExport,
    onSuccess: (data) => {
      setJobId(data.jobId)
      setPollEnabled(true)
    },
  })

  // ─── Poll export job status ───────────────────────────────────────────────
  const { data: jobStatus } = useQuery({
    queryKey: ['audit-log-export-job', jobId],
    queryFn: () => pollExportStatus(jobId!),
    enabled: pollEnabled && jobId != null,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      if (status === 'COMPLETED' || status === 'FAILED') {
        setPollEnabled(false)
        return false
      }
      return 3000  // poll every 3s (blueprints/audit-log-ui-manifest.yaml#export.poll_interval_ms)
    },
  })

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setJobId(null)
    setPollEnabled(false)
    const req: ExportRequest = {
      format,
      actorId:      filters.actorId      || undefined,
      resourceType: filters.resourceType || undefined,
      action:       filters.action       || undefined,
      outcome:      filters.outcome      || undefined,
      from:         filters.from         || undefined,
      to:           filters.to           || undefined,
    }
    mutation.mutate(req)
  }

  // ─── Access denied (AUDIT-FE-006) ────────────────────────────────────────
  if (!hasExportRole) {
    return (
      <div className="flex flex-col gap-4 max-w-lg">
        <h1 className="text-xl font-semibold">Export Audit Log</h1>
        <div className="rounded-lg border bg-muted p-4 text-sm text-muted-foreground">
          <strong>Access denied.</strong> Exporting audit logs requires the{' '}
          <code>ADMIN</code> or <code>AUDITOR</code> role. Contact your administrator.
        </div>
      </div>
    )
  }

  // ─── Main form ────────────────────────────────────────────────────────────
  return (
    <div className="flex flex-col gap-6 max-w-lg">
      <h1 className="text-xl font-semibold">Export Audit Log</h1>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {/* Format selector */}
        <div className="flex flex-col gap-1">
          <label htmlFor="export-format" className="text-sm font-medium">
            Format
          </label>
          <select
            id="export-format"
            value={format}
            onChange={(e) => setFormat(e.target.value as ExportFormat)}
            className="border rounded px-3 py-2 text-sm"
          >
            <option value="CSV">CSV</option>
            <option value="JSON">JSON</option>
          </select>
        </div>

        {/* Optional filters */}
        {[
          { key: 'actorId',      label: 'Actor ID'       },
          { key: 'resourceType', label: 'Resource Type'  },
          { key: 'action',       label: 'Action'         },
        ].map(({ key, label }) => (
          <div key={key} className="flex flex-col gap-1">
            <label htmlFor={`filter-${key}`} className="text-sm font-medium">
              {label} <span className="text-muted-foreground font-normal">(optional)</span>
            </label>
            <input
              id={`filter-${key}`}
              type="text"
              value={filters[key as keyof typeof filters] as string}
              onChange={(e) => setFilters((prev) => ({ ...prev, [key]: e.target.value }))}
              className="border rounded px-3 py-2 text-sm"
              placeholder={`Filter by ${label.toLowerCase()}`}
            />
          </div>
        ))}

        {/* Outcome filter */}
        <div className="flex flex-col gap-1">
          <label htmlFor="filter-outcome" className="text-sm font-medium">
            Outcome <span className="text-muted-foreground font-normal">(optional)</span>
          </label>
          <select
            id="filter-outcome"
            value={filters.outcome}
            onChange={(e) => setFilters((prev) => ({ ...prev, outcome: e.target.value as '' | 'SUCCESS' | 'FAILURE' }))}
            className="border rounded px-3 py-2 text-sm"
          >
            <option value="">Any</option>
            <option value="SUCCESS">Success</option>
            <option value="FAILURE">Failure</option>
          </select>
        </div>

        {/* Date range */}
        {[
          { key: 'from', label: 'From' },
          { key: 'to',   label: 'To'   },
        ].map(({ key, label }) => (
          <div key={key} className="flex flex-col gap-1">
            <label htmlFor={`filter-${key}`} className="text-sm font-medium">
              {label} <span className="text-muted-foreground font-normal">(optional)</span>
            </label>
            <input
              id={`filter-${key}`}
              type="datetime-local"
              value={filters[key as keyof typeof filters] as string}
              onChange={(e) => setFilters((prev) => ({ ...prev, [key]: e.target.value }))}
              className="border rounded px-3 py-2 text-sm"
            />
          </div>
        ))}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="bg-primary text-primary-foreground rounded px-4 py-2 text-sm font-medium hover:bg-primary/90 disabled:opacity-50"
        >
          {mutation.isPending ? 'Submitting…' : `Export as ${format}`}
        </button>
      </form>

      {/* Error state */}
      {mutation.isError && (
        <p className="text-sm text-destructive">
          Export request failed.{' '}
          {(mutation.error as Error).message === 'forbidden'
            ? 'You do not have permission to export audit logs.'
            : 'Please try again.'}
        </p>
      )}

      {/* Job status */}
      {jobId && (
        <div className="rounded-lg border p-4 flex flex-col gap-2">
          <p className="text-sm font-medium">Export Job</p>
          <p className="text-xs text-muted-foreground">Job ID: <code>{jobId}</code></p>

          {jobStatus && (
            <>
              <p className="text-sm">
                Status:{' '}
                <span className={
                  jobStatus.status === 'COMPLETED' ? 'text-green-600 font-semibold' :
                  jobStatus.status === 'FAILED'    ? 'text-red-600 font-semibold' :
                  'text-muted-foreground'
                }>
                  {jobStatus.status}
                </span>
              </p>

              {jobStatus.status === 'COMPLETED' && jobStatus.downloadUrl && (
                <a
                  href={jobStatus.downloadUrl}
                  download
                  className="text-sm underline text-primary"
                >
                  Download export ({jobStatus.recordCount?.toLocaleString()} records)
                </a>
              )}

              {jobStatus.status === 'FAILED' && (
                <p className="text-sm text-destructive">
                  Export failed: {jobStatus.errorMessage ?? 'Unknown error'}
                </p>
              )}

              {(jobStatus.status === 'PENDING' || jobStatus.status === 'PROCESSING') && (
                <p className="text-xs text-muted-foreground">Processing… checking every 3 seconds.</p>
              )}
            </>
          )}
        </div>
      )}
    </div>
  )
}

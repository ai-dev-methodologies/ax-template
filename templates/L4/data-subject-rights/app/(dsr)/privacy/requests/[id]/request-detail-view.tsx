/*
---
template_id: L4/data-subject-rights/app/(dsr)/privacy/requests/[id]/request-detail-view
layer: L4
domain: data-subject-rights
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from privacy/requests/[id]/page.tsx (BACKLOG P2-42
      render-testability pass-1 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      the page's data-fetch/mutation orchestration (useQuery/useMutation/useQueryClient) is a hard
      dependency-resolution boundary for a vitest that imports this file directly from outside
      frontend/ — the @tanstack/react-query bare specifier does not resolve for a module living in
      templates/L4/... (see frontend/tests/audit-log-redaction-render.vitest.tsx's own note on the
      same class of gap). templates/L3/pages/detail-page is React-only (zero external-npm deps —
      same precedent as (crud)/items/[id]/item-detail-view.tsx) and is safe to import here."
---
*/
import * as React from 'react'
import DetailPage from 'templates/L3/pages/detail-page/[id]/page'

// ─── types ──────────────────────────────────────────────────────────────────

export interface DsrRequest {
  requestId: string
  type: string
  status: string
  receivedAt: string
  dueAt: string
  closedAt?: string | null
  extensionDays: number
  slaBreached: boolean
}

export interface RequestDetailViewProps {
  request: DsrRequest
  extendOpen: boolean
  onToggleExtendOpen: () => void
  extensionReason: string
  onExtensionReasonChange: (value: string) => void
  onConfirmExtend: () => void
  extendPending: boolean
  error: string | null
}

// ─── helpers ─────────────────────────────────────────────────────────────────

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * RequestDetailView — pure presentational render of a single DSR request's tracking envelope.
 *
 * Deliberately has ZERO data-fetching/mutation dependencies (no useQuery/useMutation) — the
 * caller (`privacy/requests/[id]/page.tsx`) owns loading/error/extend-mutation orchestration and
 * passes the resolved `request` + extend-form state in. This keeps the component a plain
 * props -> JSX function, which is what makes it renderable in a unit test without a
 * QueryClientProvider.
 */
export default function RequestDetailView({
  request,
  extendOpen,
  onToggleExtendOpen,
  extensionReason,
  onExtensionReasonChange,
  onConfirmExtend,
  extendPending,
  error,
}: RequestDetailViewProps) {
  const sectionsSlot = (
    <div className="space-y-4">
      <dl className="divide-y rounded-lg border bg-card">
        <div className="grid grid-cols-3 gap-4 px-6 py-4">
          <dt className="text-sm font-medium text-muted-foreground">Right</dt>
          <dd className="col-span-2 text-sm">{request.type}</dd>
        </div>
        <div className="grid grid-cols-3 gap-4 px-6 py-4">
          <dt className="text-sm font-medium text-muted-foreground">Status</dt>
          <dd className="col-span-2 text-sm">{request.status}</dd>
        </div>
        <div className="grid grid-cols-3 gap-4 px-6 py-4">
          <dt className="text-sm font-medium text-muted-foreground">Received</dt>
          <dd className="col-span-2 text-sm">{formatDate(request.receivedAt)}</dd>
        </div>
        <div className="grid grid-cols-3 gap-4 px-6 py-4">
          <dt className="text-sm font-medium text-muted-foreground">Due by</dt>
          <dd className="col-span-2 text-sm">
            {formatDate(request.dueAt)}
            {request.slaBreached && <span className="ml-2 text-destructive">(overdue)</span>}
            {request.extensionDays > 0 && (
              <span className="ml-2 text-muted-foreground">(+{request.extensionDays}d extension)</span>
            )}
          </dd>
        </div>
        {request.closedAt && (
          <div className="grid grid-cols-3 gap-4 px-6 py-4">
            <dt className="text-sm font-medium text-muted-foreground">Closed</dt>
            <dd className="col-span-2 text-sm">{formatDate(request.closedAt)}</dd>
          </div>
        )}
      </dl>

      {error && (
        <div role="alert" className="rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      )}

      {extendOpen && (
        <div className="space-y-2 rounded-lg border bg-card px-6 py-4">
          <label htmlFor="extension-reason" className="block text-sm font-medium">
            Reason for extension (up to 60 days)
          </label>
          <textarea
            id="extension-reason"
            value={extensionReason}
            onChange={(e) => onExtensionReasonChange(e.target.value)}
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            rows={3}
            placeholder="Why is more time needed (complexity / volume)?"
          />
          <button
            type="button"
            onClick={onConfirmExtend}
            disabled={extendPending || !extensionReason.trim()}
            className="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
          >
            {extendPending ? 'Extending…' : 'Confirm extension'}
          </button>
        </div>
      )}
    </div>
  )

  const actionsSlot = (
    <button
      type="button"
      onClick={onToggleExtendOpen}
      className="inline-flex items-center rounded-md border bg-background px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
    >
      Extend window
    </button>
  )

  return (
    <DetailPage
      title={`Request ${request.requestId.slice(0, 8)}`}
      backHref="/privacy"
      backLabel="Back to my requests"
      actionsSlot={actionsSlot}
      sectionsSlot={sectionsSlot}
    />
  )
}

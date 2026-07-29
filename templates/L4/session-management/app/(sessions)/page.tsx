/*
---
template_id: L4/session-management/app/(sessions)/page
layer: L4
domain: session-management
domain_mode: full_trio
backend_operation_id: listMySessions
evidence:
  - source_type: internal
    rationale: "L4 session-management vertical — caller-only session inventory page. Per-user devices with masked IP + summarized UA; revoke + revoke-others actions."
  - source_type: external
    citation: "TanStack Query v5 — useQuery + useMutation for server-state"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - source_type: external
    citation: "OWASP ASVS V3 — Session Management Verification Requirements"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
'use client'

import * as React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import VirtualizedTable, { type ColumnDef } from 'templates/L2/blocks/virtualized-table'
import SessionManagementView, {
  type SessionListResponse,
  type SessionSummary,
} from './session-management-view'

// ─── column definitions ───────────────────────────────────────────────────────

const COLUMNS: ColumnDef<SessionSummary>[] = [
  {
    key: 'deviceLabel',
    header: 'Device',
    render: (row) => row.deviceLabel ?? '—',
    sortable: false,
  },
  {
    key: 'userAgentSummary',
    header: 'Browser / OS',
    render: (row) => (
      <span className="text-sm text-muted-foreground">{row.userAgentSummary}</span>
    ),
    sortable: false,
  },
  {
    key: 'ipAddressMasked',
    header: 'IP (masked)',
    render: (row) => (
      <span className="font-mono text-sm">{row.ipAddressMasked}</span>
    ),
    sortable: false,
  },
  {
    key: 'lastSeenAt',
    header: 'Last seen',
    render: (row) =>
      row.lastSeenAt ? new Date(row.lastSeenAt).toLocaleString() : '—',
    sortable: true,
  },
  {
    key: 'status',
    header: 'Status',
    render: (row) => (
      <span
        className={
          row.status === 'ACTIVE'
            ? 'text-green-600 font-medium'
            : 'text-muted-foreground'
        }
      >
        {row.status}
      </span>
    ),
    sortable: true,
  },
]

// ─── data fetching ───────────────────────────────────────────────────────────

async function fetchMySessions(): Promise<SessionListResponse> {
  const res = await fetch('/api/sessions')
  if (!res.ok) throw new Error(`Failed to load sessions (HTTP ${res.status})`)
  return res.json()
}

async function revokeSession(id: string): Promise<void> {
  const res = await fetch(`/api/sessions/${id}`, { method: 'DELETE' })
  // RFC 9110 §9.3.5: 204 on absent is also success (anchors R38
  // http-delete-idempotency-rfc9110 rule). DO NOT treat 404 / 204 / 200
  // differently from the success path.
  if (!res.ok && res.status !== 204) {
    throw new Error(`Failed to revoke session (HTTP ${res.status})`)
  }
}

async function revokeOthers(): Promise<{ revoked: number; kept: number }> {
  const res = await fetch('/api/sessions/revoke-others', { method: 'POST' })
  if (!res.ok) throw new Error(`Failed to revoke other sessions (HTTP ${res.status})`)
  return res.json()
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * MySessionsPage — caller-only session inventory + revoke actions.
 *
 * Anchored to R38 rules:
 *   - caller-authentication-only-no-userid-param: this endpoint NEVER
 *     accepts a `?userId=` query parameter. The server derives the user
 *     id from Authentication.getName(). Do NOT add a user-id filter UI.
 *   - pii-masked-at-dto-boundary: only masked IP + summarized UA are
 *     shown. The raw values exist on the backend entity as @JsonIgnore.
 *   - http-delete-idempotency-rfc9110: revokeSession() treats 204 as
 *     success regardless of prior state (see implementation above).
 *
 * Fork instructions:
 *   1. Replace `fetch('/api/sessions')` with your typed client.
 *   2. The "Revoke others" action keeps the current device's session and
 *      revokes the rest — surface a confirmation dialog before firing it.
 *   3. Add a deviceLabel input on first login so the rows have a
 *      human-readable name; without it the masked IP is the only identifier.
 */
export default function MySessionsPage() {
  const qc = useQueryClient()
  const { data, error, isLoading } = useQuery({
    queryKey: ['my-sessions'],
    queryFn: fetchMySessions,
  })

  const revoke = useMutation({
    mutationFn: revokeSession,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['my-sessions'] }),
  })

  const revokeOthersMutation = useMutation({
    mutationFn: revokeOthers,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['my-sessions'] }),
  })

  const columns = React.useMemo<ColumnDef<SessionSummary>[]>(
    () => [
      ...COLUMNS,
      {
        key: 'actions' as keyof SessionSummary,
        header: '',
        render: (row) =>
          row.status === 'ACTIVE' ? (
            <button
              type="button"
              className="text-sm text-red-600 hover:underline disabled:opacity-50"
              disabled={revoke.isPending}
              onClick={(e) => {
                e.stopPropagation()
                revoke.mutate(row.id)
              }}
            >
              Revoke
            </button>
          ) : null,
        sortable: false,
      },
    ],
    [revoke],
  )

  return (
    <SessionManagementView
      data={data}
      error={error as Error | null}
      isLoading={isLoading}
      tableSlot={data ? <VirtualizedTable data={data.items} columns={columns} /> : null}
      onRevokeOthers={() => revokeOthersMutation.mutate()}
      revokeOthersPending={revokeOthersMutation.isPending}
    />
  )
}

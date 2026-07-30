/*
---
template_id: L4/email-outbox/app/(admin)/email-outbox/page
layer: L4
domain: email-outbox
domain_mode: full_trio
backend_operation_id: listEmailOutbox
evidence:
  - source_type: internal
    rationale: "L4 email-outbox vertical — admin outbox monitor. 10s background poll + per-row Retry/Delete with R50 destructive-action-confirm + R50 stored-server-error-sanitize + R47 hooks-before-conditional-return / aria-busy / optimistic snapshot-rollback."
  - source_type: external
    citation: "TanStack Query v5 — useQuery + useMutation"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API5:2023 BFLA"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
'use client'

import * as React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import { useCallerId, useCallerRole } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError, sanitizeStoredError } from 'templates/L0/fork-receiver-kit/parse-error'
import EmailOutboxView, {
  type OutboxStatus,
  type OutboxResponse,
  type OutboxPage,
} from './email-outbox-view'
import { applyOptimisticDelete } from './email-outbox-cache'

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchOutbox(status: OutboxStatus | ''): Promise<OutboxPage> {
  const params = new URLSearchParams()
  if (status) params.set('status', status)
  const qs = params.toString()
  const res = await fetch(`/api/admin/email-outbox${qs ? `?${qs}` : ''}`)
  if (!res.ok) throw await parseError(res, 'Failed to load outbox')
  return res.json()
}

async function retryOutbox(id: string): Promise<OutboxResponse> {
  const res = await fetch(`/api/admin/email-outbox/${encodeURIComponent(id)}/retry`, {
    method: 'POST',
  })
  if (!res.ok) throw await parseError(res, 'Failed to retry')
  return res.json()
}

async function deleteOutbox(id: string): Promise<void> {
  const res = await fetch(`/api/admin/email-outbox/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
  if (!res.ok) throw await parseError(res, 'Failed to delete')
}

// ─── page ────────────────────────────────────────────────────────────────────

export default function EmailOutboxPage() {
  useCallerId()
  const role = useCallerRole()
  const qc = useQueryClient()

  // ─── all hooks ABOVE role gate (R47 hooks-before-conditional-return) ────

  const [statusFilter, setStatusFilter] = React.useState<OutboxStatus | ''>('')
  const [pendingRetryIds, setPendingRetryIds] = React.useState<Set<string>>(() => new Set())
  const [pendingDeleteIds, setPendingDeleteIds] = React.useState<Set<string>>(() => new Set())

  const { data, error, isLoading, dataUpdatedAt, refetch } = useQuery({
    queryKey: ['email-outbox', statusFilter],
    queryFn: () => fetchOutbox(statusFilter),
    // R50 incident-dashboard-background-poll-plus-refresh:
    refetchInterval: 10_000,
    refetchIntervalInBackground: true,
  })

  const retry = useMutation({
    mutationFn: retryOutbox,
    onMutate: (id: string) => {
      setPendingRetryIds((prev) => {
        const next = new Set(prev)
        next.add(id)
        return next
      })
    },
    onSettled: (_data, _err, id) => {
      setPendingRetryIds((prev) => {
        const next = new Set(prev)
        next.delete(id)
        return next
      })
      qc.invalidateQueries({ queryKey: ['email-outbox'] })
    },
  })

  const del = useMutation({
    mutationFn: deleteOutbox,
    onMutate: async (id: string) => {
      setPendingDeleteIds((prev) => {
        const next = new Set(prev)
        next.add(id)
        return next
      })
      // R47 optimistic-update-snapshot-rollback: drop the row from cache
      // immediately, restore on error. Query cache stores an OutboxPage
      // pagination envelope (content[] + totalElements etc.), not a bare
      // array — applyOptimisticDelete filters `content` and decrements
      // totalElements, preserving the rest of the envelope.
      await qc.cancelQueries({ queryKey: ['email-outbox', statusFilter] })
      const previous = qc.getQueryData<OutboxPage>(['email-outbox', statusFilter])
      qc.setQueryData<OutboxPage>(['email-outbox', statusFilter], (old) =>
        applyOptimisticDelete(old, id),
      )
      return { previous }
    },
    onError: (_err, _id, ctx) => {
      if (ctx?.previous) qc.setQueryData(['email-outbox', statusFilter], ctx.previous)
      qc.invalidateQueries({ queryKey: ['email-outbox'] })
    },
    onSettled: (_data, _err, id) => {
      setPendingDeleteIds((prev) => {
        const next = new Set(prev)
        next.delete(id)
        return next
      })
      qc.invalidateQueries({ queryKey: ['email-outbox'] })
    },
  })

  if (role !== 'admin') {
    return (
      <EmptyState
        title="Admin access required"
        description="Email outbox is administrator-only. Ask an admin to grant your account ROLE_ADMIN."
      />
    )
  }

  return (
    <ErrorBoundary>
      <EmailOutboxView
        data={data}
        error={error as Error | null}
        isLoading={isLoading}
        dataUpdatedAt={dataUpdatedAt}
        statusFilter={statusFilter}
        onStatusFilterChange={setStatusFilter}
        onRefetch={() => refetch()}
        retryError={retry.error as Error | null}
        onDismissRetryError={() => retry.reset()}
        deleteError={del.error as Error | null}
        onDismissDeleteError={() => del.reset()}
        pendingRetryIds={pendingRetryIds}
        pendingDeleteIds={pendingDeleteIds}
        onRetry={(id) => retry.mutate(id)}
        onDelete={(id) => del.mutate(id)}
        sanitizeStoredError={sanitizeStoredError}
      />
    </ErrorBoundary>
  )
}

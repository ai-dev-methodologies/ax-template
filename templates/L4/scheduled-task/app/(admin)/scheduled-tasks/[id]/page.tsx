/*
---
template_id: L4/scheduled-task/app/(admin)/scheduled-tasks/[id]/page
layer: L4
domain: scheduled-task
domain_mode: full_trio
backend_operation_id: getTaskHistory
evidence:
  - source_type: internal
    rationale: "L4 scheduled-task vertical — per-task execution history with 10s background poll. errorMessage from JobHistory passes through sanitizeStoredError (R48 lesson) so server-supplied stack-trace fragments are not screen-shared raw during incident review."
  - source_type: external
    citation: "TanStack Query v5 — refetchInterval"
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
import { useParams, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import EmptyState from 'templates/L2/blocks/empty-state'
import { useCallerId, useCallerRole } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'
import ScheduledTaskHistoryView, {
  type TaskResponse,
  type HistoryRow,
} from './scheduled-task-history-view'

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchTask(id: string): Promise<TaskResponse> {
  const res = await fetch(`/api/admin/scheduled-tasks/${encodeURIComponent(id)}`)
  if (!res.ok) throw await parseError(res, 'Failed to load task')
  return res.json()
}

async function fetchHistory(id: string): Promise<HistoryRow[]> {
  const res = await fetch(`/api/admin/scheduled-tasks/${encodeURIComponent(id)}/history`)
  if (!res.ok) throw await parseError(res, 'Failed to load history')
  return res.json()
}

// ─── page ────────────────────────────────────────────────────────────────────

export default function ScheduledTaskHistoryPage() {
  useCallerId()
  const role = useCallerRole()
  const params = useParams<{ id: string }>()
  const router = useRouter()
  const id = params.id

  // ─── all hooks ABOVE the role gate ─────────────────────────────────────────

  const task = useQuery({
    queryKey: ['scheduled-task', id],
    queryFn: () => fetchTask(id),
  })

  const history = useQuery({
    queryKey: ['scheduled-task-history', id],
    queryFn: () => fetchHistory(id),
    // R48 lesson (F5): poll continues in background tabs so a second-
    // monitor incident-bridge view stays current. Without this the v5
    // default pauses on hidden tabs.
    refetchInterval: 10_000,
    refetchIntervalInBackground: true,
  })

  if (role !== 'admin') {
    return (
      <EmptyState
        title="Admin access required"
        description="Scheduled task history is administrator-only. Ask an admin to grant your account ROLE_ADMIN."
      />
    )
  }

  return (
    <ScheduledTaskHistoryView
      task={task.data}
      taskLoading={task.isLoading}
      taskError={task.error as Error | null}
      history={history.data}
      historyLoading={history.isLoading}
      historyError={history.error as Error | null}
      historyDataUpdatedAt={history.dataUpdatedAt}
      onBack={() => router.push('/admin/scheduled-tasks')}
      onRefetchHistory={() => history.refetch()}
    />
  )
}

/*
---
template_id: L4/scheduled-task/app/(admin)/scheduled-tasks/[id]/page
layer: L4
domain: scheduled-task
domain_mode: full_trio
backend_operation_id: getScheduledTaskHistory
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
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import { useCallerId, useCallerRole } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError, sanitizeStoredError } from 'templates/L0/fork-receiver-kit/parse-error'

// ─── types ───────────────────────────────────────────────────────────────────

type TaskStatus = 'ENABLED' | 'DISABLED'
type JobOutcome = 'SUCCESS' | 'FAILURE' | 'SKIPPED'

interface TaskResponse {
  id: string
  name: string
  cronExpression: string
  status: TaskStatus
  handlerBean: string
  lastRunAt: string | null
  createdAt: string
  updatedAt: string
}

interface HistoryRow {
  id: string
  taskName: string
  startedAt: string
  finishedAt: string | null
  outcome: JobOutcome
  errorMessage: string | null
  hostInstance: string
}

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

// ─── helpers ──────────────────────────────────────────────────────────────────

function outcomeClass(o: JobOutcome): string {
  switch (o) {
    case 'SUCCESS':
      return 'bg-green-100 text-green-900'
    case 'FAILURE':
      return 'bg-red-100 text-red-900'
    case 'SKIPPED':
      return 'bg-muted text-muted-foreground'
  }
}

function durationMs(startedAt: string, finishedAt: string | null): string {
  if (!finishedAt) return 'running…'
  const ms = Math.max(0, new Date(finishedAt).getTime() - new Date(startedAt).getTime())
  if (ms < 1000) return `${ms}ms`
  if (ms < 60_000) return `${(ms / 1000).toFixed(1)}s`
  return `${Math.floor(ms / 60_000)}m ${Math.floor((ms % 60_000) / 1000)}s`
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
    <ErrorBoundary>
      <div className="space-y-6">
        <div className="flex justify-end">
          <button
            type="button"
            className="rounded border px-3 py-1 text-xs hover:bg-muted"
            onClick={() => router.push('/admin/scheduled-tasks')}
          >
            ← Back to task list
          </button>
        </div>

        {task.isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading task…
          </div>
        ) : task.error ? (
          <EmptyState
            title="Failed to load task"
            description={(task.error as Error).message}
          />
        ) : !task.data ? (
          <EmptyState title="Not found" description="This task does not exist or you do not have access." />
        ) : (
          <header className="space-y-1">
            <h1 className="text-lg font-semibold">{task.data.name}</h1>
            <div className="text-sm text-muted-foreground">
              cron <code>{task.data.cronExpression}</code> · handler{' '}
              <code>{task.data.handlerBean}</code> ·{' '}
              <span
                className={
                  task.data.status === 'ENABLED' ? 'text-green-700' : 'text-muted-foreground'
                }
              >
                {task.data.status}
              </span>
            </div>
          </header>
        )}

        <section>
          <div className="mb-2 flex items-baseline justify-between">
            <h2 className="text-sm font-semibold uppercase text-muted-foreground">
              Recent executions
            </h2>
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              {history.dataUpdatedAt
                ? `Updated ${new Date(history.dataUpdatedAt).toLocaleTimeString()}`
                : ''}
              <button
                type="button"
                className="rounded border px-2 py-1 hover:bg-muted"
                onClick={() => history.refetch()}
              >
                Refresh
              </button>
            </div>
          </div>

          {history.isLoading ? (
            <div className="py-8 text-center text-sm text-muted-foreground">
              Loading history…
            </div>
          ) : history.error ? (
            <EmptyState
              title="Failed to load history"
              description={(history.error as Error).message}
            />
          ) : !history.data || history.data.length === 0 ? (
            <EmptyState
              title="No execution history yet"
              description="This task has not run since the history retention window started."
            />
          ) : (
            <ul className="divide-y rounded border">
              {history.data.map((h) => (
                <li key={h.id} className="flex items-start gap-3 px-4 py-3">
                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex items-center gap-2">
                      <span
                        className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] uppercase ${outcomeClass(h.outcome)}`}
                      >
                        {h.outcome}
                      </span>
                      <span className="truncate text-sm">
                        started {new Date(h.startedAt).toLocaleString()}
                      </span>
                      <span className="shrink-0 text-xs text-muted-foreground">
                        · {durationMs(h.startedAt, h.finishedAt)}
                      </span>
                    </div>
                    <div className="text-xs text-muted-foreground">
                      host <code>{h.hostInstance}</code>
                    </div>
                    {h.errorMessage && (
                      <div className="rounded border border-red-200 bg-red-50/50 px-2 py-1 text-xs text-red-800">
                        {/* R48 lesson (F4): defense-in-depth — apply
                             PII/secret deny-list before rendering. */}
                        error: <code>{sanitizeStoredError(h.errorMessage)}</code>
                      </div>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </ErrorBoundary>
  )
}

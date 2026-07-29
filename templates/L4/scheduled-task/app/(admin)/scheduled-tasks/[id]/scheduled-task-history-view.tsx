/*
---
template_id: L4/scheduled-task/app/(admin)/scheduled-tasks/[id]/scheduled-task-history-view
layer: L4
domain: scheduled-task
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (admin)/scheduled-tasks/[id]/page.tsx (BACKLOG
      P2-42 render-testability pass-1 closure — same class as (crud)/items/[id]/item-detail-view.
      tsx): the page's data-fetch orchestration (two useQuery calls) is a hard
      dependency-resolution boundary for a vitest that imports this file directly from outside
      frontend/ — the @tanstack/react-query bare specifier does not resolve for a module living in
      templates/L4/... (see frontend/tests/audit-log-redaction-render.vitest.tsx's own note on the
      same class of gap). templates/L2/blocks/{empty-state,error-boundary} and
      templates/L0/fork-receiver-kit/{parse-error} have zero external-npm deps and are safe to
      import/render directly."
---
*/
import * as React from 'react'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import { sanitizeStoredError } from 'templates/L0/fork-receiver-kit/parse-error'

// ─── types ───────────────────────────────────────────────────────────────────

export type TaskStatus = 'ENABLED' | 'DISABLED'
export type JobOutcome = 'SUCCESS' | 'FAILURE' | 'SKIPPED'

export interface TaskResponse {
  id: string
  name: string
  cronExpression: string
  status: TaskStatus
  handlerBean: string
  lastRunAt: string | null
  createdAt: string
  updatedAt: string
}

export interface HistoryRow {
  id: string
  taskName: string
  startedAt: string
  finishedAt: string | null
  outcome: JobOutcome
  errorMessage: string | null
  hostInstance: string
}

export interface ScheduledTaskHistoryViewProps {
  task: TaskResponse | undefined
  taskLoading: boolean
  taskError: Error | null
  history: HistoryRow[] | undefined
  historyLoading: boolean
  historyError: Error | null
  historyDataUpdatedAt: number
  onBack: () => void
  onRefetchHistory: () => void
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

// ─── component ──────────────────────────────────────────────────────────────

/**
 * ScheduledTaskHistoryView — pure presentational render of a task's execution history.
 *
 * Deliberately has ZERO data-fetching dependencies (no useQuery) — the caller
 * (`(admin)/scheduled-tasks/[id]/page.tsx`) owns both query states (task + history) and passes
 * the resolved data in. This keeps the component a plain props -> JSX function, which is what
 * makes it renderable in a unit test without a QueryClientProvider.
 */
export default function ScheduledTaskHistoryView({
  task,
  taskLoading,
  taskError,
  history,
  historyLoading,
  historyError,
  historyDataUpdatedAt,
  onBack,
  onRefetchHistory,
}: ScheduledTaskHistoryViewProps) {
  return (
    <ErrorBoundary>
      <div className="space-y-6">
        <div className="flex justify-end">
          <button
            type="button"
            className="rounded border px-3 py-1 text-xs hover:bg-muted"
            onClick={onBack}
          >
            ← Back to task list
          </button>
        </div>

        {taskLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading task…
          </div>
        ) : taskError ? (
          <EmptyState
            title="Failed to load task"
            description={taskError.message}
          />
        ) : !task ? (
          <EmptyState title="Not found" description="This task does not exist or you do not have access." />
        ) : (
          <header className="space-y-1">
            <h1 className="text-lg font-semibold">{task.name}</h1>
            <div className="text-sm text-muted-foreground">
              cron <code>{task.cronExpression}</code> · handler{' '}
              <code>{task.handlerBean}</code> ·{' '}
              <span
                className={
                  task.status === 'ENABLED' ? 'text-green-700' : 'text-muted-foreground'
                }
              >
                {task.status}
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
              {historyDataUpdatedAt
                ? `Updated ${new Date(historyDataUpdatedAt).toLocaleTimeString()}`
                : ''}
              <button
                type="button"
                className="rounded border px-2 py-1 hover:bg-muted"
                onClick={onRefetchHistory}
              >
                Refresh
              </button>
            </div>
          </div>

          {historyLoading ? (
            <div className="py-8 text-center text-sm text-muted-foreground">
              Loading history…
            </div>
          ) : historyError ? (
            <EmptyState
              title="Failed to load history"
              description={historyError.message}
            />
          ) : !history || history.length === 0 ? (
            <EmptyState
              title="No execution history yet"
              description="This task has not run since the history retention window started."
            />
          ) : (
            <ul className="divide-y rounded border">
              {history.map((h) => (
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

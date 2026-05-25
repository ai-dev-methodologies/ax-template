/*
---
template_id: L4/scheduled-task/app/(admin)/scheduled-tasks/page
layer: L4
domain: scheduled-task
domain_mode: full_trio
backend_operation_id: listScheduledTasks
evidence:
  - source_type: internal
    rationale: "L4 scheduled-task vertical — admin task list with enable/disable toggle + manual trigger. Admin-gated via useCallerRole. Trigger requires confirm (destructive-by-side-effect — running a cron job out of cycle can mutate downstream state and conflict with the DatabaseAdvisoryLock if another instance is already running it)."
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
import { useRouter } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import { useCallerId, useCallerRole } from '../../use-caller-id'
import { parseError } from '../../parse-error'

// ─── types ───────────────────────────────────────────────────────────────────

type TaskStatus = 'ENABLED' | 'DISABLED'
type TriggerOutcome = 'SUCCESS' | 'FAILURE' | 'SKIPPED'

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

interface HistoryResponse {
  id: string
  taskName: string
  startedAt: string
  finishedAt: string | null
  outcome: TriggerOutcome
  errorMessage: string | null
  hostInstance: string
}

interface TriggerResponse {
  executed: boolean
  history: HistoryResponse | null
  reason: string | null
}

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchTasks(): Promise<TaskResponse[]> {
  const res = await fetch('/api/admin/scheduled-tasks')
  if (!res.ok) throw await parseError(res, 'Failed to load tasks')
  return res.json()
}

async function enableTask(id: string): Promise<TaskResponse> {
  const res = await fetch(`/api/admin/scheduled-tasks/${encodeURIComponent(id)}/enable`, {
    method: 'POST',
  })
  if (!res.ok) throw await parseError(res, 'Failed to enable task')
  return res.json()
}

async function disableTask(id: string): Promise<TaskResponse> {
  const res = await fetch(`/api/admin/scheduled-tasks/${encodeURIComponent(id)}/disable`, {
    method: 'POST',
  })
  if (!res.ok) throw await parseError(res, 'Failed to disable task')
  return res.json()
}

async function triggerTask(id: string): Promise<TriggerResponse> {
  const res = await fetch(`/api/admin/scheduled-tasks/${encodeURIComponent(id)}/trigger`, {
    method: 'POST',
  })
  if (!res.ok) throw await parseError(res, 'Failed to trigger task')
  return res.json()
}

// ─── helpers ──────────────────────────────────────────────────────────────────

function timeAgo(iso: string, now: Date): string {
  const ms = Math.max(0, now.getTime() - new Date(iso).getTime())
  const min = ms / 60_000
  if (min < 1) return 'just now'
  if (min < 60) return `${Math.floor(min)}m ago`
  const hr = min / 60
  if (hr < 24) return `${Math.floor(hr)}h ago`
  const d = hr / 24
  if (d < 30) return `${Math.floor(d)}d ago`
  return new Date(iso).toLocaleDateString()
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * ScheduledTasksPage — admin task list.
 *
 * R47/R48 invariants preempted:
 *   - hooks-before-conditional-return: all useQuery/useMutation/useState
 *     above the role gate.
 *   - rbac-stub-default-fail-closed: useCallerRole defaults to 'user'.
 *   - mutation-in-flight-uses-aria-busy: aria-busy + aria-disabled + click
 *     guards; native disabled NOT used for in-flight state.
 *   - error-message-not-in-native-title-attribute: errors render in
 *     role='alert' aria-live spans with Dismiss + .reset(); button title
 *     carries only the aria-label.
 *   - optimistic-update-snapshot-rollback: enable/disable mutations snapshot
 *     the cache + restore on error.
 *   - client-must-not-fabricate-audit-timestamps: lastRunAt rendered
 *     as-received; never written client-side.
 *   - Destructive-action confirm (R48 F3): manual trigger asks for
 *     confirmation — out-of-cycle execution can mutate downstream state.
 */
export default function ScheduledTasksPage() {
  useCallerId()
  const role = useCallerRole()
  const router = useRouter()
  const qc = useQueryClient()

  // ─── all hooks ABOVE the role gate ─────────────────────────────────────────

  const [now, setNow] = React.useState(() => new Date())
  React.useEffect(() => {
    const handle = () => setNow(new Date())
    window.addEventListener('focus', handle)
    return () => window.removeEventListener('focus', handle)
  }, [])

  const { data, error, isLoading } = useQuery({
    queryKey: ['scheduled-tasks'],
    queryFn: fetchTasks,
  })

  // R47 optimistic-update-snapshot-rollback: enable / disable mutations
  // flip the cache immediately, restore from snapshot on failure.
  function buildToggleMutation(
    method: 'enable' | 'disable',
    nextStatus: TaskStatus,
    apiFn: (id: string) => Promise<TaskResponse>,
  ) {
    return {
      mutationFn: apiFn,
      onMutate: async (id: string) => {
        await qc.cancelQueries({ queryKey: ['scheduled-tasks'] })
        const previous = qc.getQueryData<TaskResponse[]>(['scheduled-tasks'])
        qc.setQueryData<TaskResponse[]>(['scheduled-tasks'], (old) =>
          old ? old.map((t) => (t.id === id ? { ...t, status: nextStatus } : t)) : old,
        )
        return { previous }
      },
      onError: (_err: Error, _id: string, ctx?: { previous: TaskResponse[] | undefined }) => {
        if (ctx?.previous) qc.setQueryData(['scheduled-tasks'], ctx.previous)
        qc.invalidateQueries({ queryKey: ['scheduled-tasks'] })
      },
      onSettled: () => qc.invalidateQueries({ queryKey: ['scheduled-tasks'] }),
      _label: method,
    }
  }

  const enable = useMutation(buildToggleMutation('enable', 'ENABLED', enableTask))
  const disable = useMutation(buildToggleMutation('disable', 'DISABLED', disableTask))

  const [pendingTriggerIds, setPendingTriggerIds] = React.useState<Set<string>>(
    () => new Set(),
  )

  const [triggerOutcome, setTriggerOutcome] = React.useState<{
    taskId: string
    executed: boolean
    reason: string | null
  } | null>(null)

  const trigger = useMutation({
    mutationFn: triggerTask,
    onMutate: (id: string) => {
      setPendingTriggerIds((prev) => {
        const next = new Set(prev)
        next.add(id)
        return next
      })
      setTriggerOutcome(null)
    },
    onSuccess: (resp, id) => {
      // Backend returns `executed: false` + reason when the distributed
      // advisory lock blocked the run (another instance was already
      // running it). Surface that explicitly — operator pressed Trigger
      // but no work happened.
      setTriggerOutcome({ taskId: id, executed: resp.executed, reason: resp.reason })
    },
    onSettled: (_data, _err, id) => {
      setPendingTriggerIds((prev) => {
        const next = new Set(prev)
        next.delete(id)
        return next
      })
      qc.invalidateQueries({ queryKey: ['scheduled-tasks'] })
    },
  })

  // ─── role gate (after hooks) ──────────────────────────────────────────────

  if (role !== 'admin') {
    return (
      <EmptyState
        title="Admin access required"
        description="Scheduled task management is administrator-only. Ask an admin to grant your account ROLE_ADMIN."
      />
    )
  }

  return (
    <ErrorBoundary>
      <div className="space-y-4">
        <header>
          <h1 className="text-lg font-semibold">Scheduled tasks</h1>
          <p className="text-sm text-muted-foreground">
            Cron-driven background jobs. Disable to suspend the schedule
            (in-flight runs are not interrupted). Trigger runs a job out of cycle —
            the backend's distributed advisory lock prevents concurrent execution
            across instances.
          </p>
        </header>

        {triggerOutcome && (
          <div
            role="status"
            aria-live="polite"
            className={`flex items-start justify-between gap-3 rounded border px-3 py-1.5 text-sm ${
              triggerOutcome.executed
                ? 'border-green-300 bg-green-50 text-green-900'
                : 'border-amber-300 bg-amber-50 text-amber-900'
            }`}
          >
            <span>
              {triggerOutcome.executed
                ? 'Trigger accepted — job queued for execution.'
                : `Trigger skipped — ${triggerOutcome.reason ?? 'another instance is running this task'}`}
            </span>
            <button
              type="button"
              className="shrink-0 text-xs underline"
              onClick={() => setTriggerOutcome(null)}
            >
              Dismiss
            </button>
          </div>
        )}

        {(enable.error || disable.error || trigger.error) && (
          <div className="space-y-1.5">
            {enable.error && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Enable failed: {enable.error.message}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={() => enable.reset()}
                >
                  Dismiss
                </button>
              </div>
            )}
            {disable.error && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Disable failed: {disable.error.message}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={() => disable.reset()}
                >
                  Dismiss
                </button>
              </div>
            )}
            {trigger.error && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Trigger failed: {trigger.error.message}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={() => trigger.reset()}
                >
                  Dismiss
                </button>
              </div>
            )}
          </div>
        )}

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading tasks…
          </div>
        ) : error ? (
          <EmptyState title="Failed to load tasks" description={(error as Error).message} />
        ) : !data || data.length === 0 ? (
          <EmptyState
            title="No scheduled tasks registered"
            description="Tasks are registered by the application at startup. If you expect tasks to be here, check the backend's ScheduledTaskLoop bootstrap logs."
          />
        ) : (
          <ul className="divide-y rounded border">
            {data.map((t) => {
              const togglePending = enable.isPending || disable.isPending
              const isEnabled = t.status === 'ENABLED'
              const isTriggering = pendingTriggerIds.has(t.id)
              const handleToggle = () => {
                if (togglePending) return
                if (isEnabled) disable.mutate(t.id)
                else enable.mutate(t.id)
              }
              const handleTrigger = () => {
                if (isTriggering) return
                // R48 lesson (F3): destructive-by-side-effect — out-of-cycle
                // execution can mutate downstream state, fire webhooks, send
                // notifications, etc. Confirm before firing.
                const ok = window.confirm(
                  `Trigger "${t.name}" out of cycle now?\n\nThe handler bean ${t.handlerBean} will run on the next available worker. Side effects (db writes, webhook fires, notifications) will execute as if this were the scheduled run.`,
                )
                if (!ok) return
                trigger.mutate(t.id)
              }
              return (
                <li key={t.id} className="flex items-start gap-3 px-4 py-3">
                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex items-center gap-2">
                      <span
                        className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] uppercase ${
                          isEnabled
                            ? 'bg-green-100 text-green-900'
                            : 'bg-muted text-muted-foreground'
                        }`}
                      >
                        {t.status}
                      </span>
                      <button
                        type="button"
                        className="truncate text-left text-sm font-medium hover:underline"
                        onClick={() =>
                          router.push(`/admin/scheduled-tasks/${t.id}`)
                        }
                      >
                        {t.name}
                      </button>
                    </div>
                    <div className="text-xs text-muted-foreground">
                      cron <code>{t.cronExpression}</code> · handler{' '}
                      <code>{t.handlerBean}</code>
                      {t.lastRunAt && (
                        <>
                          {' '}· last run {timeAgo(t.lastRunAt, now)}
                        </>
                      )}
                    </div>
                  </div>
                  <div className="flex shrink-0 gap-2">
                    <button
                      type="button"
                      className="rounded border px-2 py-1 text-xs hover:bg-muted aria-busy:opacity-60 aria-disabled:opacity-50"
                      aria-busy={togglePending || undefined}
                      aria-disabled={togglePending || undefined}
                      aria-label={
                        isEnabled
                          ? `Disable scheduled task ${t.name}`
                          : `Enable scheduled task ${t.name}`
                      }
                      onClick={handleToggle}
                    >
                      {togglePending ? 'Updating…' : isEnabled ? 'Disable' : 'Enable'}
                    </button>
                    <button
                      type="button"
                      className="rounded border border-amber-300 px-2 py-1 text-xs text-amber-900 hover:bg-amber-50 aria-busy:opacity-60 aria-disabled:opacity-50"
                      aria-busy={isTriggering || undefined}
                      aria-disabled={isTriggering || undefined}
                      aria-label={`Trigger ${t.name} now`}
                      onClick={handleTrigger}
                    >
                      {isTriggering ? 'Triggering…' : 'Trigger now'}
                    </button>
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </ErrorBoundary>
  )
}

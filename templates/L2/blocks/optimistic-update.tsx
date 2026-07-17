/*
---
template_id: L2/blocks/optimistic-update
layer: L2
provenance_class: internal_design
transport: polling   # default; SSE opt-in via blueprints/realtime-policy-manifest.yaml
evidence:
  - source_type: external
    citation: "TanStack Query docs — Optimistic Updates (Mutation)"
    url: "https://tanstack.com/query/v5/docs/framework/react/guides/optimistic-updates"
  - source_type: internal
    rationale: "L2 optimistic-update block — wrapper that snapshots, applies optimistic state, and rolls back on mutation failure."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

'use client'

import * as React from 'react'

// ─── types ────────────────────────────────────────────────────────────────────

export interface OptimisticUpdateOptions<T> {
  /** Async mutation function (e.g. API call). Must resolve to updated value. */
  mutationFn: (update: Partial<T>) => Promise<T>
  /** Called after successful mutation with the confirmed server value */
  onSuccess?: (confirmed: T) => void
  /** Called after rollback with the error and the snapshot that was restored */
  onRollback?: (error: unknown, snapshot: T) => void
}

export interface OptimisticUpdateResult<T> {
  /** Current value — optimistically updated or rolled back */
  value: T
  /** Apply an optimistic update. Snapshot is saved for rollback. */
  apply: (update: Partial<T>) => Promise<void>
  /** True while the mutation is in-flight */
  isPending: boolean
  /** True if the last mutation failed and was rolled back */
  didRollback: boolean
  /** Error from the last failed mutation; null on success */
  error: unknown
}

/**
 * useOptimisticUpdate — optimistic state update hook with automatic rollback.
 *
 * **Pattern:**
 * 1. Snapshot current value.
 * 2. Merge `update` into current value immediately (optimistic).
 * 3. Call `mutationFn(update)`.
 * 4. On success: replace with server-confirmed value.
 * 5. On failure: restore snapshot, call `onRollback`.
 *
 * **Transport note:** works with both polling (default) and SSE (opt-in).
 * When SSE is active, server-pushed events may confirm the mutation before
 * the mutation promise resolves — the hook handles both orderings correctly.
 *
 * **Concurrent calls:** `apply()` may be invoked again before a prior call has
 * settled. Pending state is tracked with an in-flight counter (not a single
 * shared boolean), and each call captures its own local rollback snapshot —
 * so one call settling does not resync the value to the stale `initialValue`
 * prop or roll back a sibling call's still-pending optimistic update.
 *
 * @example
 * const { value, apply, isPending } = useOptimisticUpdate(task, {
 *   mutationFn: (update) => api.patch(`/api/tasks/${task.id}`, update),
 *   onRollback: (err) => toast.error('Failed to save — changes reverted'),
 * })
 *
 * <TaskCard
 *   task={value}
 *   onStatusChange={(status) => apply({ status })}
 *   isUpdating={isPending}
 * />
 */
export function useOptimisticUpdate<T extends object>(
  initialValue: T,
  options: OptimisticUpdateOptions<T>
): OptimisticUpdateResult<T> {
  const [value, setValue] = React.useState<T>(initialValue)
  // In-flight counter (not a single shared boolean) — apply() may be called
  // again before a prior call settles, and each concurrent call must keep
  // "pending" true until ALL of them have settled, not just the first to finish.
  const [pendingCount, setPendingCount] = React.useState(0)
  const [didRollback, setDidRollback] = React.useState(false)
  const [error, setError] = React.useState<unknown>(null)
  const isPending = pendingCount > 0

  // Keep value in sync with external changes (e.g. SSE or polling refresh).
  // Gated on the in-flight counter (not a per-call boolean) — if this only
  // fired once "isPending" flipped false, a call settling while a sibling
  // call is still in flight would resync to the stale initialValue prop and
  // silently discard the sibling's still-pending optimistic update.
  React.useEffect(() => {
    if (pendingCount === 0) setValue(initialValue)
  }, [initialValue, pendingCount])

  const apply = React.useCallback(async (update: Partial<T>) => {
    // Local per-call snapshot — NOT a shared ref. A shared ref would be
    // overwritten by a second apply() call before the first one's rollback
    // runs, causing the first call to roll back to the second call's
    // pre-state instead of its own.
    const snapshot = value
    setValue(prev => ({ ...prev, ...update }))
    setPendingCount(count => count + 1)
    setDidRollback(false)
    setError(null)

    try {
      const confirmed = await options.mutationFn(update)
      setValue(confirmed)
      options.onSuccess?.(confirmed)
    } catch (err) {
      setValue(snapshot)
      setDidRollback(true)
      setError(err)
      options.onRollback?.(err, snapshot)
    } finally {
      setPendingCount(count => count - 1)
    }
  }, [value, options])

  return { value, apply, isPending, didRollback, error }
}

// ─── OptimisticUpdateProvider (render-prop wrapper) ───────────────────────────

export interface OptimisticUpdateProviderProps<T extends object> {
  initialValue: T
  mutationFn: (update: Partial<T>) => Promise<T>
  onSuccess?: (confirmed: T) => void
  onRollback?: (error: unknown, snapshot: T) => void
  children: (result: OptimisticUpdateResult<T>) => React.ReactNode
}

/**
 * OptimisticUpdateProvider — render-prop variant for class consumers or JSX-first usage.
 *
 * @example
 * <OptimisticUpdateProvider
 *   initialValue={task}
 *   mutationFn={(update) => api.patch(`/api/tasks/${task.id}`, update)}
 *   onRollback={() => toast.error('Update failed — reverted')}
 * >
 *   {({ value, apply, isPending }) => (
 *     <TaskRow task={value} onUpdate={apply} loading={isPending} />
 *   )}
 * </OptimisticUpdateProvider>
 */
export function OptimisticUpdateProvider<T extends object>({
  initialValue,
  mutationFn,
  onSuccess,
  onRollback,
  children,
}: OptimisticUpdateProviderProps<T>) {
  const result = useOptimisticUpdate(initialValue, { mutationFn, onSuccess, onRollback })
  return <>{children(result)}</>
}

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
  const [isPending, setIsPending] = React.useState(false)
  const [didRollback, setDidRollback] = React.useState(false)
  const [error, setError] = React.useState<unknown>(null)
  const snapshotRef = React.useRef<T>(initialValue)

  // Keep value in sync with external changes (e.g. SSE or polling refresh)
  React.useEffect(() => {
    if (!isPending) setValue(initialValue)
  }, [initialValue, isPending])

  const apply = React.useCallback(async (update: Partial<T>) => {
    snapshotRef.current = value
    setValue(prev => ({ ...prev, ...update }))
    setIsPending(true)
    setDidRollback(false)
    setError(null)

    try {
      const confirmed = await options.mutationFn(update)
      setValue(confirmed)
      options.onSuccess?.(confirmed)
    } catch (err) {
      setValue(snapshotRef.current)
      setDidRollback(true)
      setError(err)
      options.onRollback?.(err, snapshotRef.current)
    } finally {
      setIsPending(false)
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

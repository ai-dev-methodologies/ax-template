/**
 * P0-29 audit closure — useOptimisticUpdate shared-state data loss on
 * double-click (two overlapping apply() calls).
 *
 * Pre-fix defect (templates/L2/blocks/optimistic-update.tsx):
 *   - A single hook-wide `isPending` boolean + a single shared `snapshotRef`
 *     were used for every apply() call. When apply(A) was in flight and
 *     apply(B) fired before A settled, whichever call's `finally` ran FIRST
 *     flipped the shared `isPending` to false even though the other call was
 *     still pending. That flip fed a resync effect
 *     (`if (!isPending) setValue(initialValue)`), snapping the value back to
 *     the stale `initialValue` prop while the sibling call's optimistic
 *     update was still outstanding — visible data loss, no error surfaced.
 *   - The shared `snapshotRef` was also overwritten by the second call
 *     before the first call's rollback ran, so a failing call would roll
 *     back to the WRONG pre-state (a sibling call's snapshot, not its own).
 *
 * Fix: an in-flight counter (increment on apply start, decrement on
 * settle) drives `isPending` and gates the resync effect — it only fires
 * when the counter is zero. Each apply() call captures its rollback
 * snapshot in a local `const`, not a shared ref.
 */
import { describe, it, expect } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useOptimisticUpdate } from '../../../templates/L2/blocks/optimistic-update'

interface Task {
  status: string
  title: string
}

// Stable module-level reference. The hook's resync effect intentionally
// re-syncs `value` whenever the caller passes a NEW `initialValue` object
// (e.g. after a fresh fetch) — that is correct, unrelated behavior. Tests
// here isolate the in-flight/rollback fix, so `initialValue` must stay
// referentially stable across renders like a real caller (e.g. a query
// result object) would.
const INITIAL: Task = { status: 'todo', title: 'Write report' }

function deferred<T>() {
  let resolve!: (v: T) => void
  let reject!: (e: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

describe('useOptimisticUpdate — concurrent apply() calls (P0-29)', () => {
  it('does not snap the value back to initialValue while a sibling call is still pending', async () => {
    const defA = deferred<Task>()
    const defB = deferred<Task>()
    let callIndex = 0

    const { result } = renderHook(() =>
      useOptimisticUpdate<Task>(INITIAL, {
        mutationFn: () => {
          callIndex += 1
          return callIndex === 1 ? defA.promise : defB.promise
        },
      })
    )

    // apply(A) — optimistic status change, mutation left in flight.
    act(() => {
      void result.current.apply({ status: 'in_progress' })
    })
    expect(result.current.value).toEqual({ status: 'in_progress', title: 'Write report' })
    expect(result.current.isPending).toBe(true)

    // apply(B) fires before A resolves — optimistic title change.
    act(() => {
      void result.current.apply({ title: 'Write final report' })
    })
    expect(result.current.value).toEqual({ status: 'in_progress', title: 'Write final report' })
    expect(result.current.isPending).toBe(true)

    // A settles first. Pre-fix, A's `finally { setIsPending(false) }` would
    // flip the SHARED isPending to false, firing the resync effect and
    // snapping value back to INITIAL even though B is still outstanding.
    // (A's confirmed payload includes B's field too — isolating the
    // pending-counter/resync fix under test from the separate, documented
    // "full-object overwrite on success" behavior, which this fix does not
    // change.)
    await act(async () => {
      defA.resolve({ status: 'in_progress', title: 'Write final report' })
      await defA.promise
    })

    // Both optimistic updates must still be visible — no snap-back to
    // initialValue, and isPending must remain true because B is still
    // in flight (in-flight counter, not a shared boolean).
    expect(result.current.value).not.toEqual(INITIAL)
    expect(result.current.value.title).toBe('Write final report')
    expect(result.current.isPending).toBe(true)

    // B settles — pending clears only once ALL in-flight calls are done.
    // (Once idle, the resync effect legitimately re-syncs `value` to the
    // `initialValue` prop per the hook's documented "keep in sync with
    // external changes" contract — real callers refresh that prop from
    // their own data source after a mutation settles. This test's INITIAL
    // is a fixed constant, standing in for a caller who has not done that,
    // so a resync-to-INITIAL here is expected, not a regression.)
    await act(async () => {
      defB.resolve({ status: 'in_progress', title: 'Write final report' })
      await defB.promise
    })
    expect(result.current.isPending).toBe(false)
  })

  it('rolls back each call to its OWN pre-state, not a snapshot clobbered by a sibling call', async () => {
    const defA = deferred<Task>()
    const defB = deferred<Task>()
    let callIndex = 0
    const rollbackSnapshots: Task[] = []

    const { result } = renderHook(() =>
      useOptimisticUpdate<Task>(INITIAL, {
        mutationFn: () => {
          callIndex += 1
          return callIndex === 1 ? defA.promise : defB.promise
        },
        onRollback: (_err, snapshot) => {
          rollbackSnapshots.push(snapshot)
        },
      })
    )

    // apply(A): pre-state is INITIAL.
    act(() => {
      void result.current.apply({ status: 'in_progress' })
    })
    // apply(B) fires before A settles: with a SHARED ref, capturing B's
    // snapshot here would overwrite the ref A still needs for its own
    // rollback.
    act(() => {
      void result.current.apply({ title: 'Write final report' })
    })

    // A fails. It must roll back to ITS OWN pre-state (INITIAL) — not to
    // whatever the shared ref held after B's apply() call overwrote it.
    await act(async () => {
      defA.reject(new Error('boom'))
      await defA.promise.catch(() => {})
    })

    expect(rollbackSnapshots).toHaveLength(1)
    expect(rollbackSnapshots[0]).toEqual(INITIAL)

    // B still resolves independently and correctly.
    await act(async () => {
      defB.resolve({ status: 'todo', title: 'Write final report' })
      await defB.promise
    })
    expect(result.current.isPending).toBe(false)
  })
})

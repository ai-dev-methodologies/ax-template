/*
---
template_id: L0/fork-receiver-kit/use-conflict-resolution
layer: L0
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "RFC 9110 — HTTP Semantics §15.5.10 (409 Conflict) + §15.5.13 (412 Precondition Failed): conditional writes on a versioned resource"
    url: "https://www.rfc-editor.org/rfc/rfc9110#section-15.5.13"
  - source_type: external
    citation: "RFC 6585 — Additional HTTP Status Codes §3 (428 Precondition Required): an origin server requires the request to be conditional (If-Match)"
    url: "https://www.rfc-editor.org/rfc/rfc6585#section-3"
  - source_type: external
    citation: "RFC 9457 — Problem Details for HTTP APIs §3.1 (extension members) — the conflict body carries a stable `code` and (for 412) a `current_etag` extension member"
    url: "https://datatracker.ietf.org/doc/html/rfc9457"
  - source_type: internal
    rationale: "FDW2 (2nd frontend dogfood) frontier finding: the backend ships the FULL optimistic-locking contract (common/OptimisticLockingSupport + GlobalProblemDetailAdvice → 428 PRECONDITION_REQUIRED, 412 PRECONDITION_FAILED + current_etag, 409 OPTIMISTIC_LOCK_CONFLICT) yet the frontend catalog had ZERO counterpart — every conditional-write UI must hand-roll the same status→intent classification and the read-fresh-then-reconcile flow. This is the identical gap-shape FMW2 closed for BulkResult: a one-sided backend contract with no frontend seam. The hook detects the three conflict statuses, reads the canonical `code`/`current_etag`, and refetches the authoritative server value so the L2 conflict-banner can present your-value vs server-value vs the latest validator."
imports_from: []
imports_forbidden: [L1, L2, L3, L4, app/, lib/]
---
*/
'use client'

import * as React from 'react'

/**
 * The three RFC conflict outcomes a conditional write on a versioned resource
 * can produce, mapped from the backend's canonical status + ProblemDetail
 * `code` (common/GlobalProblemDetailAdvice):
 *
 * - `precondition-required` — 428, code `PRECONDITION_REQUIRED`: the mutation
 *   arrived without an `If-Match` validator (RFC 6585 §3). The client must
 *   GET the current state to obtain an ETag, then retry conditionally.
 * - `stale` — 412, code `PRECONDITION_FAILED`: the supplied `If-Match` no
 *   longer matches; the resource changed under you (RFC 9110 §15.5.13). The
 *   body carries the authoritative `current_etag`.
 * - `conflict` — 409, code `OPTIMISTIC_LOCK_CONFLICT`: two writers passed the
 *   `If-Match` check and both flushed; the `@Version` bump lost the race at
 *   flush time (RFC 9110 §15.5.10).
 */
export type ConflictKind = 'precondition-required' | 'stale' | 'conflict'

/** Status + canonical `code` for each conflict kind (backend contract). */
const STATUS_TO_KIND: Record<number, ConflictKind> = {
  428: 'precondition-required',
  412: 'stale',
  409: 'conflict',
}

/** A classified conflict, before the fresh server value is fetched. */
export interface ConflictSignal {
  kind: ConflictKind
  /** The HTTP status that produced this signal (428 / 412 / 409). */
  status: number
  /** Stable ProblemDetail `code` (e.g. `PRECONDITION_FAILED`); falls back to the kind. */
  code: string
  /**
   * The authoritative current ETag the server reported (412 carries it as the
   * `current_etag` member). Adopt this as the next `If-Match` before retrying.
   */
  currentEtag?: string
}

/** A conflict enriched with both sides, ready for the L2 conflict-banner. */
export interface ConflictState<T> extends ConflictSignal {
  /** The freshly-refetched authoritative server state. */
  serverValue: T
  /** The value the user was trying to write when the conflict surfaced. */
  yourValue: T
}

/** Read a string member off an unknown ProblemDetail body without throwing. */
function readString(body: unknown, ...keys: string[]): string | undefined {
  if (!body || typeof body !== 'object') return undefined
  for (const k of keys) {
    const v = (body as Record<string, unknown>)[k]
    if (typeof v === 'string' && v.length > 0) return v
  }
  return undefined
}

/**
 * classifyConflict — pure transform of a response `status` + already-parsed
 * ProblemDetail body into a {@link ConflictSignal}, or `null` when the status
 * is not one of the three conflict codes. Lets you branch on a conflict in a
 * mutation's error path without any React or `fetch` coupling (and makes the
 * mapping unit-testable).
 */
export function classifyConflict(status: number, body?: unknown): ConflictSignal | null {
  const kind = STATUS_TO_KIND[status]
  if (!kind) return null
  return {
    kind,
    status,
    code: readString(body, 'code') ?? kind,
    currentEtag: readString(body, 'current_etag', 'currentEtag'),
  }
}

/**
 * parseConflict — classify a failed `fetch` Response. Returns `null` when the
 * status is not 428/412/409 (the caller then falls through to its normal
 * error handling, e.g. parse-error / parse-field-errors). Clones the response
 * so the body can still be read elsewhere.
 */
export async function parseConflict(res: Response): Promise<ConflictSignal | null> {
  if (!STATUS_TO_KIND[res.status]) return null
  let body: unknown
  try {
    body = await res.clone().json()
  } catch {
    body = undefined
  }
  return classifyConflict(res.status, body)
}

export interface UseConflictResolutionOptions<T> {
  /**
   * Fetch the CURRENT authoritative server state. Called when a conflict is
   * detected so the banner can show server-value vs your-value. Should return
   * the same shape you pass as `yourValue`.
   */
  refetch: () => Promise<T>
}

export interface ConflictResolution<T> {
  /** The active conflict (your-value + server-value + validator), or `null`. */
  conflict: ConflictState<T> | null
  /**
   * Inspect a failed mutation Response. If it is a 428/412/409, refetch the
   * fresh server value, store the full {@link ConflictState}, and resolve
   * `true`. Otherwise leave state untouched and resolve `false` so the caller
   * runs its normal error path.
   */
  resolveFromResponse: (res: Response, yourValue: T) => Promise<boolean>
  /** Store an already-classified signal (when you parsed the body yourself). */
  resolveFromSignal: (signal: ConflictSignal, yourValue: T) => Promise<void>
  /** Clear the active conflict (e.g. after the user reloads or overwrites). */
  dismiss: () => void
}

/**
 * useConflictResolution — manage the read-fresh-then-reconcile flow for an
 * optimistic-locking conflict. Pair with the backend's
 * `common/OptimisticLockingSupport` (428/412/409 + `current_etag`) and render
 * the result with the L2 `conflict-banner`.
 *
 * @example
 *   const { conflict, resolveFromResponse, dismiss } = useConflictResolution({
 *     refetch: () => api.getItem(id),
 *   })
 *   const mutation = useMutation({
 *     mutationFn: (next) => api.updateItem(id, next, { ifMatch: etag }),
 *     onError: async (err) => {
 *       if (err instanceof HttpError && await resolveFromResponse(err.response, draft)) return
 *       setBanner(err.message) // not a conflict — normal error path
 *     },
 *   })
 *   return conflict ? <ConflictBanner conflict={conflict} onReload={dismiss} /> : null
 */
export function useConflictResolution<T>(
  options: UseConflictResolutionOptions<T>,
): ConflictResolution<T> {
  const { refetch } = options
  const [conflict, setConflict] = React.useState<ConflictState<T> | null>(null)

  const resolveFromSignal = React.useCallback(
    async (signal: ConflictSignal, yourValue: T) => {
      const serverValue = await refetch()
      setConflict({ ...signal, serverValue, yourValue })
    },
    [refetch],
  )

  const resolveFromResponse = React.useCallback(
    async (res: Response, yourValue: T) => {
      const signal = await parseConflict(res)
      if (!signal) return false
      await resolveFromSignal(signal, yourValue)
      return true
    },
    [resolveFromSignal],
  )

  const dismiss = React.useCallback(() => setConflict(null), [])

  return { conflict, resolveFromResponse, resolveFromSignal, dismiss }
}

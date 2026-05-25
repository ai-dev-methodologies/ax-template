/*
---
template_id: L4/webhook/app/(admin)/webhooks/page
layer: L4
domain: webhook
domain_mode: full_trio
backend_operation_id: listWebhookEndpoints
evidence:
  - source_type: internal
    rationale: "L4 webhook vertical — admin endpoints list + register + delete. Signing secret is returned ONCE on POST and never on subsequent GETs (catalog invariant matching api-key); the create flow surfaces the secret with a 'save now, you cannot see it again' panel."
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
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'

// ─── types ───────────────────────────────────────────────────────────────────

interface EndpointResponse {
  id: string
  url: string
  active: boolean
  eventFilter: string
  createdAt: string
  updatedAt: string
}

interface EndpointWithSecret extends EndpointResponse {
  // Catalog invariant (R47 client-must-not-fabricate-audit-timestamps spirit
  // applied to plaintext secrets): the server returns signingSecret ONLY on
  // create. The client surfaces it once for the operator to copy, then
  // never re-fetches it.
  signingSecret: string
}

interface RegisterRequest {
  url: string
  eventFilter: string
}

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchEndpoints(): Promise<EndpointResponse[]> {
  const res = await fetch('/api/admin/webhook-endpoints')
  if (!res.ok) throw await parseError(res, 'Failed to load webhook endpoints')
  return res.json()
}

async function registerEndpoint(body: RegisterRequest): Promise<EndpointWithSecret> {
  const res = await fetch('/api/admin/webhook-endpoints', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw await parseError(res, 'Failed to register webhook')
  return res.json()
}

async function deleteEndpoint(id: string): Promise<void> {
  const res = await fetch(`/api/admin/webhook-endpoints/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
  // R38 http-delete-idempotency-rfc9110: res.ok covers 200-299 (incl. 204);
  // no dead branch on status !== 204.
  if (!res.ok) throw await parseError(res, 'Failed to delete webhook')
}

// ─── helpers ──────────────────────────────────────────────────────────────────

function isValidUrl(s: string): boolean {
  try {
    const u = new URL(s)
    return u.protocol === 'https:' || u.protocol === 'http:'
  } catch {
    return false
  }
}

// ─── secret reveal panel ─────────────────────────────────────────────────────

interface SecretRevealProps {
  endpoint: EndpointWithSecret
  onAcknowledge: () => void
}

/**
 * SecretRevealPanel — the one chance the operator has to copy the signing
 * secret. After the operator acknowledges (or navigates away), the secret is
 * cleared from React state — there is no second fetch path for it.
 */
function SecretRevealPanel({ endpoint, onAcknowledge }: SecretRevealProps) {
  const [copied, setCopied] = React.useState(false)
  const [copyError, setCopyError] = React.useState(false)

  // R48 iter2 (F1 high): beforeunload guard. While the panel is mounted,
  // the secret lives only in React state — a reload / tab close /
  // browser crash destroys it with no recovery path (server stores only
  // the hash). The native beforeunload prompt is the best safety net
  // the browser exposes without persisting the secret to storage
  // (which would create a second leak surface).
  React.useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault()
      // Modern browsers ignore the custom message and show a generic
      // prompt, but the returnValue assignment is what activates it.
      e.returnValue = ''
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [])

  const handleCopy = async () => {
    setCopyError(false)
    try {
      await navigator.clipboard.writeText(endpoint.signingSecret)
      setCopied(true)
    } catch {
      // R48 iter2 (F8 low): clipboard failure surfaced explicitly so
      // the operator does not assume success. The text remains visible
      // for manual selection (onFocus.select handles that path).
      setCopied(false)
      setCopyError(true)
    }
  }

  return (
    <section
      role="alert"
      className="space-y-3 rounded border-2 border-amber-400 bg-amber-50 p-4"
    >
      <h2 className="text-sm font-semibold text-amber-900">
        Save this signing secret now — it is shown ONCE and will never be displayed again.
      </h2>
      <p className="text-xs text-amber-900/90">
        The server stores only the cryptographic hash of this secret. If you lose it
        you must delete this endpoint and register a new one (which invalidates any
        downstream HMAC verifier already configured with this value).
      </p>
      <div className="flex items-center gap-2">
        <input
          type="text"
          readOnly
          className="w-full rounded border bg-white px-2 py-1 font-mono text-xs"
          value={endpoint.signingSecret}
          aria-label="Webhook signing secret (read-only)"
          onFocus={(e) => e.currentTarget.select()}
        />
        <button
          type="button"
          className="shrink-0 rounded border px-2 py-1 text-xs hover:bg-amber-100"
          onClick={handleCopy}
        >
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
      {copyError && (
        <p role="alert" className="text-xs text-red-700">
          Clipboard copy failed — select the secret manually and copy with Ctrl/Cmd-C.
        </p>
      )}
      <div className="flex justify-end">
        {/* R48 iter2 (F9 low): acknowledge button gated until the
             operator has at least confirmed a Copy. Prevents misclick
             that would discard the secret before saving it. */}
        <button
          type="button"
          className="rounded bg-foreground px-3 py-1.5 text-sm text-background aria-disabled:opacity-50"
          aria-disabled={!copied || undefined}
          title={!copied ? 'Click Copy first, or copy manually, before acknowledging' : undefined}
          onClick={() => {
            if (!copied) return
            onAcknowledge()
          }}
        >
          I have saved the secret
        </button>
      </div>
    </section>
  )
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * WebhookEndpointsPage — admin endpoint list + register + delete.
 *
 * Audit posture:
 *   - useCallerRole gates the entire surface — non-admin viewers see an
 *     "Admin access required" empty state. Server's @PreAuthorize on
 *     /api/admin/webhook-* is the source of truth (R47 rbac-stub-default
 *     -fail-closed: dev stub defaults to 'user', admin via env opt-in).
 *   - signingSecret is shown ONCE inside SecretRevealPanel; React state
 *     clears it on acknowledge. There is NO refetch path for the
 *     plaintext secret — fork-receivers MUST not add one.
 *   - Delete confirms — webhook endpoints in production are wired to
 *     external systems; an accidental delete drops their delivery stream.
 *
 * R47 invariants preempted:
 *   - hooks-before-conditional-return: all useQuery / useMutation /
 *     useState above the role gate's conditional return.
 *   - rbac-stub-default-fail-closed: useCallerRole defaults to 'user'.
 *   - mutation-in-flight-uses-aria-busy: aria-busy + aria-disabled, no
 *     native `disabled` for in-flight state.
 *   - error-message-not-in-native-title-attribute: errors render in
 *     role='alert' aria-live spans, not in button title.
 *   - optimistic-update-snapshot-rollback: delete uses onMutate snapshot
 *     + onError ctx.previous restore.
 */
export default function WebhookEndpointsPage() {
  useCallerId() // fires production hard-stop if stub not wired
  const role = useCallerRole()
  const qc = useQueryClient()

  // ─── all hooks ABOVE the role gate (Rules of Hooks) ────────────────────────

  const { data, error, isLoading } = useQuery({
    queryKey: ['webhook-endpoints'],
    queryFn: fetchEndpoints,
  })

  const [draftUrl, setDraftUrl] = React.useState('')
  // R48 iter2 (F7 low): default empty — force the operator to type a
  // narrow filter. '*' wildcard is convenient in dev but blows up
  // delivery volume in production; making it explicit is the safer default.
  const [draftFilter, setDraftFilter] = React.useState('')
  const [revealedEndpoint, setRevealedEndpoint] = React.useState<EndpointWithSecret | null>(
    null,
  )

  const register = useMutation({
    mutationFn: registerEndpoint,
    onSuccess: (resp) => {
      qc.invalidateQueries({ queryKey: ['webhook-endpoints'] })
      setRevealedEndpoint(resp)
      setDraftUrl('')
      setDraftFilter('')
    },
  })

  const del = useMutation({
    mutationFn: deleteEndpoint,
    onMutate: async (id: string) => {
      await qc.cancelQueries({ queryKey: ['webhook-endpoints'] })
      const previous = qc.getQueryData<EndpointResponse[]>(['webhook-endpoints'])
      qc.setQueryData<EndpointResponse[]>(['webhook-endpoints'], (old) =>
        old ? old.filter((e) => e.id !== id) : old,
      )
      return { previous }
    },
    onError: (_err, _id, ctx) => {
      if (ctx?.previous) qc.setQueryData(['webhook-endpoints'], ctx.previous)
      qc.invalidateQueries({ queryKey: ['webhook-endpoints'] })
    },
    onSettled: () => qc.invalidateQueries({ queryKey: ['webhook-endpoints'] }),
  })

  const urlInvalid = draftUrl.trim().length > 0 && !isValidUrl(draftUrl.trim())
  const submitBlocked =
    draftUrl.trim().length === 0 || urlInvalid || draftFilter.trim().length === 0

  // ─── role gate (after all hooks) ──────────────────────────────────────────

  if (role !== 'admin') {
    return (
      <EmptyState
        title="Admin access required"
        description="Webhook endpoints are managed by administrators. Ask an admin to grant your account ROLE_ADMIN."
      />
    )
  }

  return (
    <ErrorBoundary>
      <div className="space-y-6">
        <header>
          <h1 className="text-lg font-semibold">Webhook endpoints</h1>
          <p className="text-sm text-muted-foreground">
            URLs your platform pushes events to. Each endpoint signs requests
            with an HMAC-SHA256 using its signing secret. The secret is shown
            once on registration — save it where your verifier can find it.
          </p>
        </header>

        {revealedEndpoint && (
          <SecretRevealPanel
            endpoint={revealedEndpoint}
            onAcknowledge={() => setRevealedEndpoint(null)}
          />
        )}

        {(register.error || del.error) && (
          <div className="space-y-1.5">
            {register.error && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Register failed: {register.error.message}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={() => register.reset()}
                >
                  Dismiss
                </button>
              </div>
            )}
            {del.error && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Delete failed: {del.error.message}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={() => del.reset()}
                >
                  Dismiss
                </button>
              </div>
            )}
          </div>
        )}

        <section
          className={`rounded border p-4 ${
            revealedEndpoint ? 'pointer-events-none opacity-50' : ''
          }`}
          aria-disabled={revealedEndpoint !== null || undefined}
        >
          <h2 className="mb-2 text-sm font-semibold">Register endpoint</h2>
          {/* R48 iter2 (F2 medium): register form is visually disabled
               while a secret reveal panel is pending. Submitting a
               second registration would clobber revealedEndpoint and
               permanently lose the first secret. */}
          {revealedEndpoint && (
            <p className="mb-3 text-xs text-amber-900">
              Acknowledge the signing secret above before registering another endpoint.
            </p>
          )}
          <form
            className="space-y-3"
            onSubmit={(e) => {
              e.preventDefault()
              if (submitBlocked || revealedEndpoint) return
              register.mutate({
                url: draftUrl.trim(),
                eventFilter: draftFilter.trim(),
              })
            }}
          >
            <label className="block space-y-1">
              <span className="text-xs font-medium">Destination URL (https recommended)</span>
              <input
                type="url"
                className="w-full rounded border px-2 py-1 text-sm"
                value={draftUrl}
                onChange={(e) => setDraftUrl(e.target.value)}
                placeholder="https://example.com/hooks/incoming"
                required
              />
              {urlInvalid && (
                <span className="text-xs text-red-600">
                  Not a valid URL. Use the full <code>https://host/path</code> form.
                </span>
              )}
            </label>
            <label className="block space-y-1">
              <span className="text-xs font-medium">Event filter</span>
              <input
                type="text"
                className="w-full rounded border px-2 py-1 text-sm"
                value={draftFilter}
                onChange={(e) => setDraftFilter(e.target.value)}
                placeholder="* (all events) or e.g. order.* or payment.completed"
              />
              <span className="text-xs text-muted-foreground">
                Glob-style match against backend event types. Use <code>*</code> to receive
                everything; narrow per environment to keep delivery volume manageable.
              </span>
            </label>
            <button
              type="submit"
              className="rounded bg-foreground px-3 py-1.5 text-sm text-background aria-busy:opacity-60 aria-disabled:opacity-50"
              aria-busy={register.isPending || undefined}
              aria-disabled={submitBlocked || register.isPending || undefined}
              onClick={(e) => {
                if (submitBlocked || register.isPending) e.preventDefault()
              }}
            >
              {register.isPending ? 'Registering…' : 'Register endpoint'}
            </button>
          </form>
        </section>

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading endpoints…
          </div>
        ) : error ? (
          <EmptyState title="Failed to load endpoints" description={(error as Error).message} />
        ) : !data || data.length === 0 ? (
          <EmptyState
            title="No endpoints registered yet"
            description="Use the form above to register your first webhook destination."
          />
        ) : (
          <ul className="divide-y rounded border">
            {data.map((endpoint) => {
              const handleDelete = () => {
                if (
                  !window.confirm(
                    `Delete this webhook?\n\n${endpoint.url}\n\nDelivery to this endpoint will stop immediately. Re-creating the endpoint generates a new signing secret — your downstream verifier must be reconfigured.`,
                  )
                ) {
                  return
                }
                del.mutate(endpoint.id)
              }
              const deleting = del.isPending && del.variables === endpoint.id
              return (
                <li key={endpoint.id} className="flex items-start gap-3 px-4 py-3">
                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex items-center gap-2">
                      <span
                        className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] uppercase ${
                          endpoint.active
                            ? 'bg-green-100 text-green-900'
                            : 'bg-muted text-muted-foreground'
                        }`}
                      >
                        {endpoint.active ? 'Active' : 'Inactive'}
                      </span>
                      <span className="truncate font-mono text-xs">{endpoint.url}</span>
                    </div>
                    <div className="text-xs text-muted-foreground">
                      filter: <code>{endpoint.eventFilter}</code> · created{' '}
                      {new Date(endpoint.createdAt).toLocaleString()}
                    </div>
                  </div>
                  <button
                    type="button"
                    className="shrink-0 rounded border border-red-300 px-2 py-1 text-xs text-red-700 hover:bg-red-50 aria-busy:opacity-60 aria-disabled:opacity-50"
                    aria-busy={deleting || undefined}
                    aria-disabled={deleting || undefined}
                    aria-label={`Delete webhook endpoint ${endpoint.url}`}
                    onClick={() => {
                      if (deleting) return
                      handleDelete()
                    }}
                  >
                    {deleting ? 'Deleting…' : 'Delete'}
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </ErrorBoundary>
  )
}

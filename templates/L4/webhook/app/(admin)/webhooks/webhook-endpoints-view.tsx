/*
---
template_id: L4/webhook/app/(admin)/webhooks/webhook-endpoints-view
layer: L4
domain: webhook
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (admin)/webhooks/page.tsx (BACKLOG P2-42
      render-testability pass-2 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      the page's data-fetch/mutation orchestration (useQuery/useMutation/useQueryClient) is a hard
      dependency-resolution boundary for a vitest that imports this file directly from outside
      frontend/ — the @tanstack/react-query bare specifier does not resolve for a module living in
      templates/L4/... (see frontend/tests/audit-log-redaction-render.vitest.tsx's own note on the
      same class of gap). SecretRevealPanel (navigator.clipboard + beforeunload effect) and the
      per-row delete confirm() are pure client-side interaction concerns with zero data-fetching
      dependencies and move here unmodified — same precedent as
      frontend/tests/email-outbox-view.vitest.tsx's window.confirm-stays-in-the-view rationale.
      templates/L2/blocks/{empty-state,error-boundary} have zero external-npm deps."
---
*/
import * as React from 'react'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'

// ─── types ───────────────────────────────────────────────────────────────────

export interface EndpointResponse {
  id: string
  url: string
  active: boolean
  eventFilter: string
  createdAt: string
  updatedAt: string
}

export interface EndpointWithSecret extends EndpointResponse {
  signingSecret: string
}

export interface WebhookEndpointsViewProps {
  data: EndpointResponse[] | undefined
  error: Error | null
  isLoading: boolean

  revealedEndpoint: EndpointWithSecret | null
  onAcknowledgeReveal: () => void

  registerErrorMessage: string | null
  onDismissRegisterError: () => void
  deleteErrorMessage: string | null
  onDismissDeleteError: () => void

  draftUrl: string
  draftFilter: string
  onDraftUrlChange: (value: string) => void
  onDraftFilterChange: (value: string) => void
  onSubmitRegister: (url: string, eventFilter: string) => void
  registerPending: boolean

  onDelete: (id: string) => void
  deletePending: boolean
  deletingId: string | null
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

  React.useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault()
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

// ─── component ──────────────────────────────────────────────────────────────

/**
 * WebhookEndpointsView — pure presentational render of the admin webhook endpoints surface.
 *
 * Deliberately has ZERO data-fetching/mutation dependencies (no useQuery/useMutation) — the
 * caller (`(admin)/webhooks/page.tsx`) owns the ROLE_ADMIN gate, all query/mutation state, and
 * passes the resolved `data`, error messages, form state, and mutation-trigger callbacks in. This
 * keeps the component a plain props -> JSX function, which is what makes it renderable in a unit
 * test without a QueryClientProvider.
 */
export default function WebhookEndpointsView({
  data,
  error,
  isLoading,
  revealedEndpoint,
  onAcknowledgeReveal,
  registerErrorMessage,
  onDismissRegisterError,
  deleteErrorMessage,
  onDismissDeleteError,
  draftUrl,
  draftFilter,
  onDraftUrlChange,
  onDraftFilterChange,
  onSubmitRegister,
  registerPending,
  onDelete,
  deletePending,
  deletingId,
}: WebhookEndpointsViewProps) {
  const urlInvalid = draftUrl.trim().length > 0 && !isValidUrl(draftUrl.trim())
  const submitBlocked =
    draftUrl.trim().length === 0 || urlInvalid || draftFilter.trim().length === 0

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
          <SecretRevealPanel endpoint={revealedEndpoint} onAcknowledge={onAcknowledgeReveal} />
        )}

        {(registerErrorMessage || deleteErrorMessage) && (
          <div className="space-y-1.5">
            {registerErrorMessage && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Register failed: {registerErrorMessage}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={onDismissRegisterError}
                >
                  Dismiss
                </button>
              </div>
            )}
            {deleteErrorMessage && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Delete failed: {deleteErrorMessage}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={onDismissDeleteError}
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
              onSubmitRegister(draftUrl.trim(), draftFilter.trim())
            }}
          >
            <label className="block space-y-1">
              <span className="text-xs font-medium">Destination URL (https recommended)</span>
              <input
                type="url"
                className="w-full rounded border px-2 py-1 text-sm"
                value={draftUrl}
                onChange={(e) => onDraftUrlChange(e.target.value)}
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
                onChange={(e) => onDraftFilterChange(e.target.value)}
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
              aria-busy={registerPending || undefined}
              aria-disabled={submitBlocked || registerPending || undefined}
              onClick={(e) => {
                if (submitBlocked || registerPending) e.preventDefault()
              }}
            >
              {registerPending ? 'Registering…' : 'Register endpoint'}
            </button>
          </form>
        </section>

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading endpoints…
          </div>
        ) : error ? (
          <EmptyState title="Failed to load endpoints" description={error.message} />
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
                onDelete(endpoint.id)
              }
              const deleting = deletePending && deletingId === endpoint.id
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

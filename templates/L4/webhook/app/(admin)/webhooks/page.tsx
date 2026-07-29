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
import { useCallerId, useCallerRole } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'
import WebhookEndpointsView, {
  type EndpointResponse,
  type EndpointWithSecret,
} from './webhook-endpoints-view'

// ─── types ───────────────────────────────────────────────────────────────────

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
    <WebhookEndpointsView
      data={data}
      error={error as Error | null}
      isLoading={isLoading}
      revealedEndpoint={revealedEndpoint}
      onAcknowledgeReveal={() => setRevealedEndpoint(null)}
      registerErrorMessage={register.error?.message ?? null}
      onDismissRegisterError={() => register.reset()}
      deleteErrorMessage={del.error?.message ?? null}
      onDismissDeleteError={() => del.reset()}
      draftUrl={draftUrl}
      draftFilter={draftFilter}
      onDraftUrlChange={setDraftUrl}
      onDraftFilterChange={setDraftFilter}
      onSubmitRegister={(url, eventFilter) => register.mutate({ url, eventFilter })}
      registerPending={register.isPending}
      onDelete={(id) => del.mutate(id)}
      deletePending={del.isPending}
      deletingId={del.variables ?? null}
    />
  )
}

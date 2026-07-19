/*
 * VIOLATING FIXTURE — S2.AUTHZ.FE
 *
 * "Test delivery" is an admin-only action (it triggers a real outbound
 * server-side HTTP call to the endpoint's target URL, from an existing
 * webhook admin surface — templates/L4/webhook/app/(admin)/webhooks/page.tsx
 * gates its WHOLE page with `if (role !== 'admin') return <EmptyState/>`).
 *
 * The bug this fixture dogfoods: an AI agent adding ONE MORE admin action to
 * an existing (already-gated) surface reasonably assumes the page-level gate
 * already covers it, and ships a standalone button component that renders
 * unconditionally with NO role check of its own. If this component is ever
 * reused outside the gated page (a second admin surface, a shared toolbar,
 * a Storybook-style composition, a future refactor that inlines it into an
 * ungated layout) it fires the outbound SSRF-risk request for ANY caller,
 * not just admins — this is exactly the class of defect BFLA (OWASP
 * API5:2023) describes: function-level authorization enforced at ONE call
 * site (a page) is not enforced at the FUNCTION (the button + its handler)
 * itself.
 */
'use client'

import * as React from 'react'
import { useCallerRole } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'

interface TestWebhookDeliveryButtonProps {
  endpointId: string
}

async function triggerTestDelivery(endpointId: string): Promise<void> {
  const res = await fetch(`/api/admin/webhook-endpoints/${encodeURIComponent(endpointId)}/test`, {
    method: 'POST',
  })
  if (!res.ok) throw await parseError(res, 'Failed to send test delivery')
}

/**
 * TestWebhookDeliveryButton — VIOLATING: useCallerRole is imported but never
 * consulted. The admin-only action renders and is clickable for ANY caller
 * who mounts this component, with no gate of its own.
 */
export default function TestWebhookDeliveryButton({ endpointId }: TestWebhookDeliveryButtonProps) {
  // NOTE: role is read but never checked before the admin action renders —
  // exactly the "imported the hook, forgot the gate" shortcut this scenario
  // dogfoods.
  const role = useCallerRole()
  const [sending, setSending] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  const handleTest = async () => {
    setSending(true)
    setError(null)
    try {
      await triggerTestDelivery(endpointId)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Test delivery failed')
    } finally {
      setSending(false)
    }
  }

  return (
    <div data-caller-role={role} className="inline-flex flex-col gap-1">
      {/* ax:admin-action — triggers a real outbound server-side fetch of the
          endpoint's target URL; must never render for a non-admin caller. */}
      <button
        type="button"
        aria-busy={sending || undefined}
        aria-disabled={sending || undefined}
        className="rounded border px-2 py-1 text-xs hover:bg-muted"
        onClick={handleTest}
      >
        {sending ? 'Sending…' : 'Send test delivery'}
      </button>
      {error && (
        <span role="alert" className="text-xs text-red-700">
          {error}
        </span>
      )}
    </div>
  )
}

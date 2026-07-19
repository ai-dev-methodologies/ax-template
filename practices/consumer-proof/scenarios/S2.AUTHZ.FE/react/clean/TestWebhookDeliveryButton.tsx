/*
 * CLEAN FIXTURE — S2.AUTHZ.FE
 *
 * Same "Test delivery" admin action as the violating fixture, but the
 * component gates ITSELF (does not rely solely on being mounted inside an
 * already-gated page) — mirroring the page-level pattern already used by
 * templates/L4/webhook/app/(admin)/webhooks/page.tsx (`if (role !== 'admin')
 * return <EmptyState .../>`), applied at the component/function level per
 * OWASP API5:2023 BFLA guidance: authorize the FUNCTION, not just the page
 * that happens to embed it today.
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
 * TestWebhookDeliveryButton — CLEAN: the component checks useCallerRole()
 * itself and renders nothing for a non-admin caller, BEFORE the admin-only
 * action markup. The gate travels with the component wherever it is
 * mounted — it does not depend on an enclosing page's own gate.
 */
export default function TestWebhookDeliveryButton({ endpointId }: TestWebhookDeliveryButtonProps) {
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

  // Function-level gate (R47 rbac-stub-default-fail-closed spirit applied
  // at component granularity): this MUST come before the marker below.
  if (role !== 'admin') {
    return null
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

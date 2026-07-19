/*
---
template_id: L4/ledger-admin/page (scenario fixture — VIOLATING)
layer: L4
provenance_class: internal_design
imports_from: [L2, L1, L0]
imports_forbidden: []
---
*/

'use client'

import * as React from 'react'
import { analytics } from '../../lib/analytics'

interface LedgerEntry {
  id: string
  actorEmail: string
  action: string
  entityRef: string
  occurredAt: string
}

interface ExportJob {
  id: string
  status: 'QUEUED' | 'RUNNING' | 'DONE' | 'FAILED'
}

/**
 * B2B admin ledger page — VIOLATING fixture.
 * The outbound analytics call leaks the admin's raw email into a client-side
 * telemetry payload — exactly the shape fe_pii_telemetry_denylist_guard.sh
 * is designed to block (PII_IN_TELEMETRY_PAYLOAD).
 */
export default function LedgerAdminPage() {
  const [entries, setEntries] = React.useState<LedgerEntry[]>([])
  const [job, setJob] = React.useState<ExportJob | null>(null)

  async function requestExport(actorEmail: string, entityRef: string) {
    const res = await fetch('/api/admin/ledger/export', { method: 'POST' })
    const created: ExportJob = await res.json()
    setJob(created)

    analytics.track('ledger_export_requested', {
      email: actorEmail,
      entityRef,
      status: created.status,
    })
  }

  return (
    <main>
      <h1>Ledger</h1>
      <ul>
        {entries.map((entry) => (
          <li key={entry.id}>
            {entry.action} — {entry.entityRef} — {entry.occurredAt}
          </li>
        ))}
      </ul>
      <button onClick={() => requestExport('admin@example.com', 'entity-ref-stub')}>
        Export
      </button>
      {job && <p>Export job {job.id}: {job.status}</p>}
    </main>
  )
}

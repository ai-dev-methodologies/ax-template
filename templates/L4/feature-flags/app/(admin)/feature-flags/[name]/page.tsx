/*
---
template_id: L4/feature-flags/admin-detail
layer: L4
domain: feature-flags
provenance_class: internal_design
spec_ref: "specs/feature-flags-frontend-l0.yaml#FF-FE-003"
backend_operation_id: updateFeatureFlag
evidence:
  - source_type: external
    citation: "Next.js App Router Docs — Dynamic route segments and params"
    url: "https://nextjs.org/docs/app/building-your-application/routing/dynamic-routes"
usage: |
  Admin detail page: toggle + description editor for one flag.
  Requires ROLE_ADMIN session.
  Replace 'YOUR_API_BASE' with your backend URL or use next.config.ts rewrites.
---
*/
import { FeatureFlagToggle } from '@/templates/L2/blocks/feature-flag-toggle'
import Link from 'next/link'
import { notFound } from 'next/navigation'

interface FeatureFlag {
  name: string
  enabled: boolean
  description: string | null
  createdAt: string
  updatedAt: string
}

async function fetchFlag(
  apiBase: string,
  name: string,
): Promise<FeatureFlag | null> {
  // Admin GET single not in OpenAPI — reuse list and filter, or add GET /{name} endpoint
  const res = await fetch(
    `${apiBase}/api/v1/admin/feature-flags?size=1000`,
    { cache: 'no-store' },
  )
  if (!res.ok) return null
  const page = (await res.json()) as { content: FeatureFlag[] }
  return page.content.find((f) => f.name === name) ?? null
}

/**
 * Feature Flag Detail (Admin) page.
 *
 * Shows toggle + description editor for a single flag identified by [name].
 *
 * spec_ref: FF-FE-003
 * blueprint_ref: blueprints/feature-flags-ui-manifest.yaml#admin-detail
 */
export default async function FeatureFlagDetailPage({
  params,
}: {
  params: { name: string }
}) {
  const apiBase = process.env.BACKEND_API_BASE ?? ''
  const flag = await fetchFlag(apiBase, params.name)
  if (!flag) notFound()

  return (
    <main>
      <Link href="/admin/feature-flags">← Back to Feature Flags</Link>

      <h1>
        <code>{flag.name}</code>
      </h1>

      <section>
        <h2>Status</h2>
        <FeatureFlagToggle
          name={flag.name}
          initialEnabled={flag.enabled}
          apiBase={apiBase}
          label={`Toggle ${flag.name}`}
        />
      </section>

      <section>
        <h2>Description</h2>
        {/* Description edit form — submit calls PATCH /api/v1/admin/feature-flags/{name} */}
        <DescriptionForm
          name={flag.name}
          initialDescription={flag.description ?? ''}
          apiBase={apiBase}
        />
      </section>

      <section>
        <dl>
          <dt>Created</dt>
          <dd>{new Date(flag.createdAt).toLocaleString('ko-KR')}</dd>
          <dt>Last Updated</dt>
          <dd>{new Date(flag.updatedAt).toLocaleString('ko-KR')}</dd>
        </dl>
      </section>
    </main>
  )
}

// ─── Client component for description edit ────────────────────────────────────
// Keep client component co-located for simplicity (single admin page).

'use client'

function DescriptionForm({
  name,
  initialDescription,
  apiBase,
}: {
  name: string
  initialDescription: string
  apiBase: string
}) {
  const [value, setValue] = useState(initialDescription)
  const [saving, setSaving] = useState(false)
  const [status, setStatus] = useState<'idle' | 'saved' | 'error'>('idle')

  async function handleSave() {
    setSaving(true)
    setStatus('idle')
    try {
      const res = await fetch(
        `${apiBase}/api/v1/admin/feature-flags/${encodeURIComponent(name)}`,
        {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ description: value }),
        },
      )
      setStatus(res.ok ? 'saved' : 'error')
    } catch {
      setStatus('error')
    } finally {
      setSaving(false)
    }
  }

  function handleCancel() {
    setValue(initialDescription)
    setStatus('idle')
  }

  return (
    <div>
      <textarea
        value={value}
        onChange={(e) => setValue(e.target.value)}
        maxLength={500}
        rows={3}
        aria-label="Flag description"
      />
      <div>
        <button type="button" onClick={handleSave} disabled={saving}>
          {saving ? 'Saving…' : 'Save'}
        </button>
        <button
          type="button"
          onClick={handleCancel}
          disabled={saving}
        >
          Cancel
        </button>
      </div>
      {status === 'saved' && <p role="status">Saved.</p>}
      {status === 'error' && <p role="alert">Save failed.</p>}
    </div>
  )
}

// useState import for the client component above
import { useState } from 'react'

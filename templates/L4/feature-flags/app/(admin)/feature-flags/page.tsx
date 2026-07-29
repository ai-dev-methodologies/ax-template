/*
---
template_id: L4/feature-flags/admin-list
layer: L4
domain: feature-flags
provenance_class: internal_design
spec_ref: "specs/feature-flags-frontend-l0.yaml#FF-FE-001"
backend_operation_id: listFeatureFlags
evidence:
  - source_type: external
    citation: "Next.js App Router Docs — Server Components and data fetching"
    url: "https://nextjs.org/docs/app/building-your-application/data-fetching/fetching"
usage: |
  Admin page: list all feature flags with inline toggle.
  Requires ROLE_ADMIN session — protect via Next.js middleware or layout auth check.
  Replace 'YOUR_API_BASE' with your backend URL or use next.config.ts rewrites.
---
*/
import FeatureFlagsListView, { type FeatureFlag } from './feature-flags-list-view'

interface FlagPage {
  content: FeatureFlag[]
  totalElements: number
}

async function fetchFlags(apiBase: string): Promise<FlagPage> {
  const res = await fetch(`${apiBase}/api/v1/admin/feature-flags?size=100`, {
    cache: 'no-store',
  })
  if (!res.ok) throw new Error(`Failed to fetch flags: ${res.status}`)
  return res.json() as Promise<FlagPage>
}

/**
 * Feature Flags Admin List page.
 *
 * Renders a table of all flags with inline FeatureFlagToggle and a link
 * to the detail page for each flag.
 *
 * spec_ref: FF-FE-001
 * blueprint_ref: blueprints/feature-flags-ui-manifest.yaml#admin-list
 */
export default async function FeatureFlagsPage() {
  const apiBase = process.env.BACKEND_API_BASE ?? ''
  const { content: flags, totalElements } = await fetchFlags(apiBase)

  return <FeatureFlagsListView flags={flags} totalElements={totalElements} apiBase={apiBase} />
}

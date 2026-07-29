/*
---
template_id: L4/feature-flags/app/(admin)/feature-flags/feature-flags-list-view
layer: L4
domain: feature-flags
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (admin)/feature-flags/page.tsx (BACKLOG P2-42
      render-testability pass-1 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      the page is an async Server Component (fetch happens at render time, before any client
      hydration), which is itself a boundary a vitest cannot await/render the way it renders a
      Client Component. Splitting the resolved-flags->JSX render surface into its own (non-async,
      plain props) file makes it directly renderable. templates/L2/blocks/feature-flag-toggle has
      zero data-fetching-hook imports (plain fetch inside a click handler, not useQuery/useMutation)
      so it is safe to compose here. Uses a plain <a> instead of next/link's Link component for
      the per-row Edit link — next/link is unresolvable from a file outside frontend/ for the same
      reason as cmdk/@tanstack/*, and (crud)/items/[id]/item-detail-view.tsx +
      (payment)/success/[orderId]/payment-success-view.tsx already establish plain <a> as this
      catalog's own precedent for view-layer action links."
---
*/
import { FeatureFlagToggle } from 'templates/L2/blocks/feature-flag-toggle'

// ─── types ──────────────────────────────────────────────────────────────────

export interface FeatureFlag {
  name: string
  enabled: boolean
  description: string | null
  updatedAt: string
}

export interface FeatureFlagsListViewProps {
  flags: FeatureFlag[]
  totalElements: number
  apiBase: string
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * FeatureFlagsListView — pure presentational render of the admin feature-flags list.
 *
 * Deliberately has ZERO async/data-fetching orchestration — the caller
 * (`(admin)/feature-flags/page.tsx`, an async Server Component) owns the GET
 * /api/v1/admin/feature-flags fetch and passes the resolved `flags` array in. This keeps the
 * component a plain props -> JSX function, which is what makes it renderable in a unit test.
 */
export default function FeatureFlagsListView({ flags, totalElements, apiBase }: FeatureFlagsListViewProps) {
  return (
    <main>
      <h1>Feature Flags</h1>
      <p>{totalElements} flag(s) defined</p>

      {flags.length === 0 ? (
        <p>No flags defined.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Status</th>
              <th>Description</th>
              <th>Last Updated</th>
              <th>Detail</th>
            </tr>
          </thead>
          <tbody>
            {flags.map((flag) => (
              <tr key={flag.name}>
                <td>
                  <code>{flag.name}</code>
                </td>
                <td>
                  <FeatureFlagToggle
                    name={flag.name}
                    initialEnabled={flag.enabled}
                    apiBase={apiBase}
                    label={`Toggle ${flag.name}`}
                  />
                </td>
                <td>{flag.description ?? '—'}</td>
                <td>{new Date(flag.updatedAt).toLocaleString('ko-KR')}</td>
                <td>
                  <a href={`/admin/feature-flags/${flag.name}`}>Edit</a>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  )
}

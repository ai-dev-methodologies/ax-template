/*
---
template_id: L4/auth/app/(authenticated)/dashboard/dashboard-view
layer: L4
domain: auth
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (authenticated)/dashboard/page.tsx (BACKLOG
      P2-42 render-testability pass-1 closure — same class as (crud)/items/[id]/item-detail-view.
      tsx): the page's data-fetch orchestration (useEffect + fetch('/auth/me') + useRouter logout)
      is application-lifecycle wiring that a vitest rendering this view directly does not need to
      reproduce. Splitting the resolved-authState->JSX render surface into its own file makes it
      renderable without a router context or a mocked fetch. AuthState shape (role/emailVerified/
      linkedProviders) matches P1-73's GET /api/auth/me contract parity fix — see the page's own
      frontmatter/comment for the byte-for-byte contract anchor."
---
*/
import * as React from 'react'

// ─── types ──────────────────────────────────────────────────────────────────

// P1-73 — matches GET /api/auth/me byte-for-byte (UserProfileResponse{userId, email,
// role, emailVerified, linkedProviders}; contracts/auth-openapi.yaml `AuthState`).
// `role` is a single string, never a roles array; `linkedProviders` is a flat array of
// provider-name strings, never provider-link objects.
export interface AuthState {
  email: string
  role: string
  emailVerified: boolean
  linkedProviders?: string[]
}

export interface DashboardViewProps {
  authState: AuthState | null
  onLogout: () => void
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * DashboardView — pure presentational render of the auth vertical's placeholder dashboard.
 *
 * Deliberately has ZERO data-fetching dependencies (no useEffect/fetch) — the caller
 * (`(authenticated)/dashboard/page.tsx`) owns the GET /auth/me fetch and the logout flow, and
 * passes the resolved `authState` + an `onLogout` callback in. This keeps the component a plain
 * props -> JSX function, which is what makes it renderable in a unit test.
 */
export default function DashboardView({ authState, onLogout }: DashboardViewProps) {
  return (
    <div className="min-h-svh p-8">
      <div className="mx-auto max-w-2xl space-y-8">
        <header className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold">Dashboard</h1>
          <button
            type="button"
            onClick={onLogout}
            className="inline-flex items-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium shadow-sm hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            Sign out
          </button>
        </header>

        {authState ? (
          <div className="rounded-lg border p-6 space-y-3">
            <h2 className="text-sm font-medium text-muted-foreground uppercase tracking-wide">
              Account
            </h2>
            <dl className="space-y-2 text-sm">
              <div className="flex gap-2">
                <dt className="font-medium w-36 shrink-0">Email</dt>
                <dd className="text-muted-foreground">{authState.email}</dd>
              </div>
              <div className="flex gap-2">
                <dt className="font-medium w-36 shrink-0">Role</dt>
                <dd className="text-muted-foreground">{authState.role}</dd>
              </div>
              <div className="flex gap-2">
                <dt className="font-medium w-36 shrink-0">Email verified</dt>
                <dd className="text-muted-foreground">
                  {authState.emailVerified ? 'Yes' : 'Pending'}
                </dd>
              </div>
              {authState.linkedProviders && authState.linkedProviders.length > 0 && (
                <div className="flex gap-2">
                  <dt className="font-medium w-36 shrink-0">Linked providers</dt>
                  <dd className="text-muted-foreground">
                    {authState.linkedProviders.join(', ')}
                  </dd>
                </div>
              )}
            </dl>
          </div>
        ) : (
          <div className="rounded-lg border p-6">
            <p className="text-sm text-muted-foreground">Loading profile…</p>
          </div>
        )}

        {/* Fork: replace this placeholder with your actual dashboard content */}
        <div className="rounded-lg border border-dashed p-6 text-center text-sm text-muted-foreground">
          Your dashboard content goes here.
        </div>
      </div>
    </div>
  )
}

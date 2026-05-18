// TDD anchor — SP34 fixture: PASS case for impersonation-banner-required-when-acting-as-other-user
// SCENARIO: session.actingAs is set AND <ImpersonationBanner> is co-located.
// EXPECTED OUTCOME: Rule does NOT fire — banner is present.

import * as React from 'react'
// import ImpersonationBanner from 'templates/L2/blocks/impersonation-banner'

interface Session {
  operatorId: string
  actingAs: string | null
  actingAsDisplayName?: string
}

// Helper that sets actingAs (any name is fine — banner is in the layout)
export function runAsUser(userId: string, session: Session): Session {
  return { ...session, actingAs: userId }
}

// CORRECT: layout renders ImpersonationBanner whenever session.actingAs is non-null
export function AdminLayout({ session, children }: { session: Session; children: React.ReactNode }) {
  return (
    <div>
      {/* ImpersonationBanner renders iff session.actingAs !== null — SATISFIES RULE */}
      {/* <ImpersonationBanner session={session} onEndImpersonation={() => {}} /> */}
      {/* NOTE: In this pass fixture we stub the import to avoid template path resolution.
          The rule scanner looks for the <ImpersonationBanner tag in the same file/layout. */}
      <div data-component="ImpersonationBanner" data-acting-as={session.actingAs ?? undefined} />
      <main>{children}</main>
    </div>
  )
}

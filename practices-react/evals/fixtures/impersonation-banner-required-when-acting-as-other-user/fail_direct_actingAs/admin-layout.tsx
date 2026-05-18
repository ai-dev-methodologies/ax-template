// TDD anchor — SP34 fixture: FAIL case for impersonation-banner-required-when-acting-as-other-user
// SCENARIO: Direct assignment to session.actingAs without <ImpersonationBanner>.
// EXPECTED OUTCOME: Rule fires on canonical actingAs mutation.

// VIOLATION: sets session.actingAs = userId without the required impersonation banner
export async function startImpersonation(userId: string) {
  const session = await getSession()
  session.actingAs = userId  // VIOLATION: canonical actingAs direct assignment
  return session
}

async function getSession() {
  // Initial state — actingAs is null (no impersonation active)
  return { operatorId: 'op-001', actingAs: null as string | null }
}

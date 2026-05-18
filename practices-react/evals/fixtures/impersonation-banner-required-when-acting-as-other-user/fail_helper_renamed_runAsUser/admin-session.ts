// TDD anchor — SP34 fixture: FAIL case for impersonation-banner-required-when-acting-as-other-user
// SCENARIO: Helper renamed from assumeUserId() to runAsUser() — bypass attempt.
// EXPECTED OUTCOME: Rule MUST still fire because the returned object contains {actingAs: userId}.
// This fixture proves that helper-rename bypass (Critic Soft Suggestion 2) is impossible.

// VIOLATION: returns {actingAs: userId} — canonical actingAs shape detected.
// The function name "runAsUser" is irrelevant to the rule.
export function runAsUser(userId: string) {
  const currentSession = { operatorId: 'op-001', actingAs: null as string | null }
  // VIOLATION: canonical actingAs mutation via renamed helper — no <ImpersonationBanner> rendered
  return { ...currentSession, actingAs: userId }
}

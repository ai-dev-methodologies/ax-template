---
title: "Auth UI pages must realize the auth API contract — each page renders its documented fields as controlled inputs and calls its documented endpoint, with route gating, transparent token refresh, and logout cleanup"
rule_id: auth-frontend-pages-bind-auth-contract
impact: HIGH
impactDescription: "An auth surface where a page renders the wrong fields, calls the wrong endpoint, or skips a step (no email-verification page, no resend, no oauth-unlink last-provider guard) leaves the auth flow incomplete or broken; an auth-aware layout that does not gate unauthenticated users leaks protected pages; token refresh not handled in the HTTP interceptor forces re-login on every expiry; logout that does not clear local state leaves a 'logged-in' UI after sign-out. The auth UI must be a faithful 1:1 realization of the auth API contract."
tags:
  - auth
  - frontend
  - forms
  - contract-first
  - session
  - oauth
applicable_to:
  - react
  - nextjs
spec_ref: "specs/auth-frontend-l0.yaml#AUTH-FE-001"
verification:
  type: review
  notes: |
    Reviewer confirms the auth UI against specs/auth-frontend-l0.yaml and contracts/auth-openapi.yaml:
    each documented page renders its documented fields as CONTROLLED inputs (value + onChange) and calls
    its documented endpoint — signup, login (incl. OAuth provider buttons → oauthAuthorize),
    email-verification, resend-verification, forgot-password, reset-password (with token), change-password
    (current + new), oauth-callback (success AND error), account-settings (linked providers, oauth-link,
    oauth-unlink with last-provider guard). The auth-aware layout calls getAuthState on mount and
    redirects unauthenticated users to login (AUTH-FE-011). Token refresh is transparent in the HTTP
    client interceptor, no dedicated page (AUTH-FE-012). Logout clears local auth state, calls the logout
    endpoint, then redirects to login (AUTH-FE-013). No page maps to an undocumented endpoint; no
    documented endpoint lacks a page.
evidence:
  - source_type: external
    citation: "React Docs — <input> (controlled inputs): auth form fields are controlled (value + onChange) so submitted credentials come from component state (AUTH-FE-001..007)"
    url: "https://react.dev/reference/react-dom/components/input"
    quote: "To render a controlled input, pass the value prop to it (or checked for checkboxes and radios). React will force the input to always have the value you passed."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "React Docs — Reacting to input with state (declarative UI): auth pages render documented states (idle/submitting/error) declaratively"
    url: "https://react.dev/learn/reacting-to-input-with-state"
    quote: "React provides a declarative way to manipulate the UI. Instead of manipulating individual pieces of the UI directly, you describe the different states that your component can be in, and switch between them in response to the user input."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Auth UI pages must realize the auth API contract — documented fields as controlled inputs, documented endpoints, route gating, transparent refresh, logout cleanup

**Impact: HIGH — The auth frontend is a contract-first surface: every page exists to drive one endpoint of `contracts/auth-openapi.yaml`, and the flow only works if the mapping is complete and faithful. A signup page that posts to the wrong endpoint, a missing email-verification or resend page, an oauth-unlink with no last-provider guard, an auth-aware layout that forgets to redirect unauthenticated users (leaking protected pages), token refresh bolted onto a page instead of the HTTP interceptor (re-login on every expiry), or a logout that calls the endpoint but leaves the local auth state set (a 'logged-in' UI after sign-out) — each breaks the auth experience or its security. React gives the building blocks: form fields are controlled inputs — *to render a controlled input, pass the value prop to it ... React will force the input to always have the value you passed* — and pages render their states declaratively — *you describe the different states that your component can be in, and switch between them in response to the user input*.**

There are fourteen load-bearing requirements — the items of `specs/auth-frontend-l0.yaml`, all governed by this rule. Each is "the documented page renders its documented fields (as controlled inputs) and calls its documented endpoint":

- **Credential pages** — signup (AUTH-FE-001), login (AUTH-FE-002): email + password controlled fields → signup/login endpoints.
- **Verification** — email-verification token entry → verify-email (AUTH-FE-003); resend link → resend-verification (AUTH-FE-004).
- **Password** — forgot-password email → reset-request (AUTH-FE-005); reset-password new+confirm + token → reset (AUTH-FE-006); change-password current+new → change (AUTH-FE-007).
- **OAuth** — callback page handles success AND error (AUTH-FE-008); account-settings shows linked providers + oauth-link (AUTH-FE-009); oauth-unlink with a last-provider guard (AUTH-FE-010); login OAuth buttons → oauthAuthorize per enabled provider (AUTH-FE-014).
- **Session** — auth-aware layout calls getAuthState on mount and redirects unauthenticated users to login (AUTH-FE-011); token refresh is transparent in the HTTP interceptor, no dedicated page (AUTH-FE-012); logout clears local auth state, calls logout, redirects to login (AUTH-FE-013).

**Incorrect — uncontrolled field, wrong endpoint, no route gating, logout leaves state set:**

```tsx
<input name="email" />                                   {/* VIOLATION: uncontrolled (AUTH-FE-001) */}
await api.post('/users', creds);                         {/* VIOLATION: not the documented signup endpoint */}
function Layout({children}) { return <>{children}</>; }  {/* VIOLATION: no getAuthState gate (AUTH-FE-011) */}
function logout() { api.post('/auth/logout'); }          {/* VIOLATION: local auth state not cleared (AUTH-FE-013) */}
```

**Correct — controlled fields, documented endpoints, gated layout, transparent refresh, full logout:**

```tsx
const [email, setEmail] = useState('');                  // controlled (AUTH-FE-001)
<input value={email} onChange={e => setEmail(e.target.value)} type="email" />
await authClient.signup({ email, password });            // documented signup endpoint (contracts/auth-openapi.yaml)

function AuthAwareLayout({ children }) {                  // AUTH-FE-011
  const { authed } = useAuthState();                     // getAuthState on mount
  if (!authed) return <Navigate to="/login" />;          // gate unauthenticated
  return children;
}
// httpClient interceptor refreshes the token transparently (AUTH-FE-012)
function logout() { authStore.clear(); authClient.logout(); router.push('/login'); } // AUTH-FE-013
```

Verification: review-tier. Contract fidelity is a UI-to-API mapping property with no compile signal — a page calling the wrong endpoint or a missing route gate compiles and renders. Verify by review against `specs/auth-frontend-l0.yaml` + `contracts/auth-openapi.yaml`: every documented page renders its documented fields as controlled inputs and calls its documented endpoint; the layout gates unauthenticated users; token refresh is in the interceptor; logout clears state. When a fork-receiver wires real component/e2e tests (each page submits to its endpoint; unauthenticated → redirect; logout clears state), this rule's verification may be upgraded from review to a test-tag binding.

Reference: [React — <input> (controlled inputs)](https://react.dev/reference/react-dom/components/input)

Reference: [React — Reacting to input with state](https://react.dev/learn/reacting-to-input-with-state)

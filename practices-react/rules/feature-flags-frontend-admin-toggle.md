---
title: "Feature-flag admin UI must list flags with a toggle that PATCHes optimistically, an editable detail page, and server-side middleware evaluation"
rule_id: feature-flags-frontend-admin-toggle
impact: MEDIUM
impactDescription: "A flag admin table with no toggle forces a code deploy to flip a flag; a toggle that does not PATCH the server leaves the change local-only; a detail Save that does not PATCH (or a Cancel that does not discard) corrupts the edit flow; middleware that does not evaluate flags server-side lets a disabled feature's route still render. The admin surface is how a flag is operated — its mutations must reach the server."
tags:
  - feature-flags
  - frontend
  - admin
  - optimistic-update
  - middleware
applicable_to:
  - react
  - nextjs
spec_ref: "specs/feature-flags-frontend-l0.yaml#FF-FE-001"
verification:
  type: review
  notes: |
    Reviewer confirms the feature-flag admin UI against specs/feature-flags-frontend-l0.yaml: the admin
    page lists all flags in a table with name, enabled status (toggle), description, last-modified, and an
    empty state (001). The FeatureFlagToggle calls PATCH /api/v1/admin/feature-flags/{name} on interaction
    with an optimistic update + rollback on failure (002). The admin detail page renders the toggle + a
    description editor; Save calls PATCH, Cancel discards local edits (003). Middleware evaluates flags
    server-side via GET /api/v1/feature-flags/{name}/active and redirects to /not-found when disabled
    (005). (The runtime FeatureGate component is FF-FE-004, governed by prefer-feature-gate-over-env-check.)
evidence:
  - source_type: external
    citation: "React Docs — <input> (controlled inputs): the flag toggle is a controlled checkbox (checked + onChange) (FF-FE-002)"
    url: "https://react.dev/reference/react-dom/components/input"
    quote: "To render a controlled input, pass the value prop to it (or checked for checkboxes and radios). React will force the input to always have the value you passed."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "React Docs — Reacting to input with state (declarative UI): the admin table renders list/empty + optimistic/rolled-back states declaratively (FF-FE-001/002)"
    url: "https://react.dev/learn/reacting-to-input-with-state"
    quote: "React provides a declarative way to manipulate the UI. Instead of manipulating individual pieces of the UI directly, you describe the different states that your component can be in, and switch between them in response to the user input."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Feature-flag admin UI must list flags with a PATCHing optimistic toggle, an editable detail page, and server-side middleware evaluation

**Impact: MEDIUM — Feature flags exist so a feature can be flipped WITHOUT a deploy; the admin UI is how that flip happens, so its mutations must reach the server. A table with no toggle defeats the purpose; a toggle that flips local state but never PATCHes leaves the server unchanged (the flag reverts on reload); a detail Save that does not PATCH, or a Cancel that does not discard, corrupts the edit flow; and middleware that does not evaluate flags server-side lets a disabled feature's route still render to the user. React supplies the primitives — the toggle is a controlled checkbox (*to render a controlled input, pass the value prop (or checked for checkboxes) ... React will force the input to always have the value you passed*) and the table renders its states (incl. optimistic + rollback) declaratively.**

There are four load-bearing requirements here (FF-FE-004, the runtime gate, is governed by `prefer-feature-gate-over-env-check`).

**Admin list (FF-FE-001).** A table of all flags with name, enabled-status toggle, description, last-modified, and an empty state.

**Toggle PATCH + optimistic (FF-FE-002).** The FeatureFlagToggle calls `PATCH /api/v1/admin/feature-flags/{name}` on interaction, applying an optimistic update and rolling back on failure.

**Detail editor (FF-FE-003).** The admin detail page renders the toggle + a description editor; Save calls PATCH; Cancel discards local edits.

**Server-side middleware (FF-FE-005).** Middleware evaluates flags server-side via `GET /api/v1/feature-flags/{name}/active` and redirects to `/not-found` when disabled — a disabled feature's route does not render.

**Incorrect — local-only toggle; middleware does not gate the route:**

```tsx
<Toggle checked={flag.enabled} onChange={v => setLocal(v)} />   {/* VIOLATION: no PATCH → server unchanged (FF-FE-002) */}
export function middleware() { return NextResponse.next(); }    {/* VIOLATION: no server-side flag eval (FF-FE-005) */}
```

**Correct — PATCHing optimistic toggle; middleware redirects when disabled:**

```tsx
async function onToggle(name, next) {
  setOptimistic(name, next);                                    // optimistic (FF-FE-002)
  try { await api.patch(`/v1/admin/feature-flags/${name}`, { enabled: next }); }
  catch { rollback(name); }                                     // rollback on failure
}
export async function middleware(req) {                         // FF-FE-005
  const { active } = await fetch(`/v1/feature-flags/${flagFor(req)}/active`).then(r => r.json());
  return active ? NextResponse.next() : NextResponse.redirect(new URL('/not-found', req.url));
}
```

Verification: review-tier. Admin-mutation fidelity is a UI-to-API property with no compile signal. Verify by review against `specs/feature-flags-frontend-l0.yaml`: the admin table lists flags with an empty state; the toggle PATCHes with optimistic+rollback; the detail Save/Cancel behave; middleware evaluates server-side and redirects when disabled. When a fork-receiver wires real tests (toggle PATCHes; disabled route → /not-found), this rule's verification may be upgraded from review to a test-tag binding.

Reference: [React — <input> (controlled inputs)](https://react.dev/reference/react-dom/components/input)

Reference: [React — Reacting to input with state](https://react.dev/learn/reacting-to-input-with-state)

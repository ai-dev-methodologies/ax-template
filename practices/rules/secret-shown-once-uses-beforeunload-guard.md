---
title: One-time-revealed plaintext secrets MUST wire beforeunload guard for the duration of the reveal panel
impact: HIGH
impactDescription: "Plaintext secrets shown once (api-key, webhook signing secret) live only in component state — a stray reload / tab close / route navigation destroys them with no server-side recovery path"
tags:
  - secret
  - one-time-reveal
  - beforeunload
  - credential-lifecycle
  - api-key
  - webhook
spec_ref: "specs/api-key-l0.yaml#KEY-STORAGE-001"
verification:
  type: review
  source: "templates/L4/webhook/app/(admin)/webhooks/page.tsx (SecretRevealPanel), templates/L4/api-key/app/(api-key)/page.tsx (catalog plaintext-shown-once flow)"
  pattern: "useEffect(() => { window.addEventListener('beforeunload', handler) ... }, []) inside the panel component that holds the secret in React state, with returnValue assignment to trigger the native prompt"
upstream:
  - "https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "MDN Web Docs — Window: beforeunload event"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event"
    quote: "The beforeunload event is fired when the current window, contained document, and associated resources are about to be unloaded. ... To trigger the dialog, an event handler in the page should call the preventDefault() method on the event."
    quoted_at: "2026-05-25"
---

## One-time-revealed plaintext secrets MUST wire beforeunload guard for the duration of the reveal panel

**Impact: HIGH — irrecoverable secret loss is a high-friction operational hazard**

The catalog has at least two surfaces where a server-irrecoverable plaintext secret is revealed exactly once to the operator:

- **api-key** — when the admin issues a new API key, the response carries the plaintext key value. The server stores only `SHA-256(key)`. Future GETs never return the plaintext.
- **webhook** — when the admin registers a new webhook endpoint, the response carries the `signingSecret` used for HMAC-SHA256 over `<timestamp>.<body>`. The server stores only the hash. Future GETs never return it.

Similar patterns will appear in any catalog L4 that follows the "secret stored as hash" pattern — OAuth client secrets, magic-link tokens, recovery codes, signing keys.

In all of these, the plaintext lives in React component state ONLY for the duration of the reveal panel. The moment the panel unmounts (via Acknowledge click, route navigation, tab close, browser crash, or an accidental reload), the plaintext is gone with no server-side recovery. The operator must delete the endpoint and register a new one — which forces every downstream verifier to be reconfigured (multi-party coordination cost, sometimes across organization boundaries).

The catalog convention since R48 is: **wire `beforeunload` for the duration of the reveal panel**. The native browser prompt is the last line of defense against accidental reload/close. Modern browsers ignore the custom message and show a generic "Leave site? Changes you made may not be saved" — but the `returnValue` assignment is what triggers it.

**Incorrect — bare panel; reload destroys secret silently:**

```tsx
function SecretRevealPanel({ endpoint, onAcknowledge }) {
  return (
    <section>
      <input readOnly value={endpoint.signingSecret} />
      <button onClick={onAcknowledge}>I have saved the secret</button>
    </section>
  )
}
```

A pager-driven SRE hits Cmd-R out of muscle memory. The secret is gone forever. The endpoint must be deleted and recreated. Downstream Stripe/PayPal/partner verifier needs reconfiguration.

**Correct — beforeunload guard for the panel's lifetime:**

```tsx
function SecretRevealPanel({ endpoint, onAcknowledge }) {
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault()
      // Modern browsers ignore custom messages but the returnValue assignment
      // is what triggers the native prompt.
      e.returnValue = ''
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [])

  return (
    <section role="alert">
      <h2>Save this signing secret now — shown ONCE.</h2>
      <input readOnly value={endpoint.signingSecret} />
      <button onClick={onAcknowledge}>I have saved the secret</button>
    </section>
  )
}
```

**Pairs with three companion patterns**:
1. **Acknowledge gated on Copy** — the acknowledge button is `aria-disabled` until the operator clicks Copy at least once (defends against misclick on the acknowledge button itself, which is often visually close to Copy)
2. **Clipboard failure surfaced** — `navigator.clipboard.writeText` can fail silently in locked-down environments; the operator must see "Copy failed — select manually" rather than assume the copy succeeded
3. **Sibling create form disabled while reveal pending** — a second registration submitted while the panel is up would overwrite the revealed state with the new response, losing the first secret

`sessionStorage` / `localStorage` persistence is the WRONG fix — it creates a second leak surface (DevTools inspection, browser extension scraping, multi-user shared workstation). The `beforeunload` prompt is the right tradeoff: prevent accidental loss without creating a persistent attack surface.

**When to apply**: any frontend surface that displays a server-irrecoverable plaintext credential. The catalog's R48 webhook SecretRevealPanel is the reference implementation; the api-key L4 plaintext-shown-once flow follows the same pattern.

**When NOT to apply**: re-displayable credentials (OAuth tokens with `/refresh` endpoint, JWTs the server can re-issue, session cookies). The recovery cost is low; the beforeunload prompt becomes friction without benefit.

Reference: [MDN — Window: beforeunload event](https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event)

Reference: [OWASP ASVS V2.10 — Service Authentication Requirements](https://owasp.org/www-project-application-security-verification-standard/)

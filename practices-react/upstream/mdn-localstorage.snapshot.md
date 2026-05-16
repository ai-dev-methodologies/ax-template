# Snapshot: MDN — Window.localStorage

- **source**: https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage
- **role**: primitive-semantics
- **fetched_at**: 2026-05-16T00:00:00Z
- **via**: WebFetch

## Failure modes (verbatim)

> "May throw a SecurityError if storage is disabled or quota is exceeded."

> "When the storage origin is forbidden by the user agent (for example, when in private browsing mode in some browsers like Safari), calling setItem() or any other API method modifying storage will throw a SecurityError."

## Storage scope and lifetime

- Origin-scoped. `https://example.com:443` and `http://example.com:80` are separate origins.
- Persists across page reloads and browser restarts.
- Cleared by user (clear site data) or by storage pressure under certain conditions.

## Limits

- Typical quota: ~5 MB per origin (browser-dependent).
- Exceeding quota throws on `setItem`.

## SSR consideration

- `window` is undefined in Node.js / SSR contexts.
- `if (typeof window === 'undefined') return DEFAULT` is the standard guard.

## Audit implication

Any localStorage access without try-catch is a latent error in private-mode users and quota-pressed users. Any access without SSR guard breaks hydration on Next.js / SSR setups.

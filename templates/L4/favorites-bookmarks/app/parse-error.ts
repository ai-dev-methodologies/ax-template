/*
---
template_id: L4/favorites-bookmarks/app/parse-error
layer: L4
domain: favorites-bookmarks
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 favorites-bookmarks vertical — shared RFC 9457 ProblemDetail unwrap with text/html fallback + PII deny-list (R44 closure). R55 extension exposes the backend `code` property so the UI can render actionable advice for FAVORITES_QUOTA_EXCEEDED instead of a generic 400 banner."
  - source_type: external
    citation: "RFC 9457 — Problem Details for HTTP APIs"
    url: "https://datatracker.ietf.org/doc/html/rfc9457"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/

/**
 * Server-known error codes the UI may special-case for actionable advice.
 * Anchored to the catalog backend's FavoriteController error mapping —
 * extending this set is a coordinated backend+frontend change.
 */
export type FavoritesErrorCode =
  | 'FAVORITES_QUOTA_EXCEEDED'
  | 'VALIDATION_ERROR'

/**
 * Error subclass that preserves the backend ProblemDetail.code property when
 * present. parseError returns plain {@link Error} for unknown codes and
 * {@link FavoritesError} when the body carries a recognised code field — so
 * UI handlers can do `err instanceof FavoritesError && err.code === '...'`.
 */
export class FavoritesError extends Error {
  readonly code: FavoritesErrorCode | string

  constructor(message: string, code: FavoritesErrorCode | string) {
    super(message)
    this.name = 'FavoritesError'
    this.code = code
  }
}

export async function parseError(res: Response, fallback: string): Promise<Error> {
  const cloned = res.clone()
  try {
    const body = await res.json()
    const message =
      (body?.detail && String(body.detail)) ||
      (body?.message && String(body.message)) ||
      ''
    const code = typeof body?.code === 'string' ? body.code : ''
    if (code) {
      return new FavoritesError(message || fallback, code)
    }
    if (message) return new Error(message)
  } catch {
    /* JSON parse failed — try text/* next */
  }
  try {
    const raw = (await cloned.text()).trim()
    const stripped = raw.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
    const looksSensitive =
      /@[\w.-]+\.[A-Za-z]{2,}/.test(stripped) ||
      /\b(?:sk-|pk-|Bearer\s+|jdbc:|-----BEGIN |ghp_|ghs_)/i.test(stripped) ||
      /\b\d{1,3}(?:\.\d{1,3}){3}\b/.test(stripped) ||
      /\.internal\b|\.local\b/.test(stripped) ||
      // Korean enterprise PII patterns — RRN, mobile, JWT shapes commonly
      // leaked in stack-trace excerpts from Spring + Resilience4j surfaces.
      /\d{6}-\d{7}/.test(stripped) ||
      /01[016789]-?\d{3,4}-?\d{4}/.test(stripped) ||
      /eyJ[A-Za-z0-9._-]{20,}/.test(stripped)
    if (stripped.length > 0 && stripped.length <= 120 && !looksSensitive) {
      return new Error(stripped)
    }
  } catch {
    /* fall through */
  }
  return new Error(`${fallback} (HTTP ${res.status})`)
}

/*
---
template_id: L0/fork-receiver-kit/parse-error
layer: L0
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "RFC 9457 — Problem Details for HTTP APIs"
    url: "https://datatracker.ietf.org/doc/html/rfc9457"
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API3:2023 Broken Object Property Level Authorization"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/"
  - source_type: internal
    rationale: "R53 fork-receiver-kit consolidation — every L4 had its own parse-error.ts with the same RFC 9457 unwrap logic plus a Korean enterprise PII deny-list. R55 added FavoritesError (code preservation); R51/scheduled-task added sanitizeStoredError (render-layer scrub). All three converge into one canonical helper here. CodedError replaces FavoritesError — code semantics are domain-neutral, the catalog refuses to claim ownership of any specific code namespace."
imports_from: []
imports_forbidden: [L1, L2, L3, L4, app/, lib/]
---
*/

/**
 * Error subclass that preserves the backend ProblemDetail.code property when
 * present.
 *
 * Use this when the UI needs to react to a known error code (e.g. show
 * actionable advice for FAVORITES_QUOTA_EXCEEDED vs a generic 400 banner).
 * Callers narrow with {@code err instanceof CodedError && err.code === '...'}.
 *
 * The {@code code} type is intentionally {@code string} — the catalog refuses
 * to claim ownership of any specific code namespace. Each L4 owns its set
 * (and may export a typed string-literal union for IDE convenience).
 */
export class CodedError extends Error {
  readonly code: string

  constructor(message: string, code: string) {
    super(message)
    this.name = 'CodedError'
    this.code = code
  }
}

// ─── PII deny-list ───────────────────────────────────────────────────────────

/**
 * sanitizeStoredError — neutralise PII that may have ended up in a stored
 * error string. Used at the render layer when surfacing {@code lastError}
 * columns from an audit/outbox/job table to admin operators.
 *
 * <p>Korean enterprise context: RRN (주민등록번호 — 6-7 digit pattern),
 * mobile numbers (010-XXXX-XXXX), JWT shapes (eyJ...) and internal hostnames
 * (.internal / .local) regularly leak into Spring stack-trace excerpts.
 * Redacts each match with the literal {@code [REDACTED]} placeholder.
 */
export function sanitizeStoredError(raw: string | null | undefined): string {
  if (raw == null) return ''
  let s = String(raw)
  s = s.replace(/\d{6}-\d{7}/g, '[REDACTED]')                     // KR RRN
  s = s.replace(/01[016789]-?\d{3,4}-?\d{4}/g, '[REDACTED]')      // KR mobile
  s = s.replace(/eyJ[A-Za-z0-9._-]{20,}/g, '[REDACTED]')          // JWT shape
  s = s.replace(/Bearer\s+[A-Za-z0-9._-]+/gi, '[REDACTED]')       // Bearer token
  s = s.replace(/sk-[A-Za-z0-9._-]{10,}/g, '[REDACTED]')          // OpenAI-style secret
  s = s.replace(/ghp_[A-Za-z0-9]{20,}/g, '[REDACTED]')            // GitHub PAT
  s = s.replace(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g, '[REDACTED]') // email
  s = s.replace(/\b(?:\d{1,3}\.){3}\d{1,3}\b/g, '[REDACTED]')     // IPv4
  s = s.replace(/[\w-]+\.internal\b/g, '[REDACTED]')              // internal hostname
  s = s.replace(/[\w-]+\.local\b/g, '[REDACTED]')                 // .local hostname
  return s
}

// ─── parseError ──────────────────────────────────────────────────────────────

/**
 * parseError — best-effort extraction of a user-visible message from a failed
 * fetch Response.
 *
 * Resolution order:
 *   1. {@code body.detail} (RFC 9457 ProblemDetail)
 *   2. {@code body.message} (legacy convention)
 *   3. text/html fallback, stripped of tags and PII-screened
 *   4. {@code `${fallback} (HTTP ${status})`} when nothing else is safe
 *
 * When the JSON body carries a {@code code} string, the returned error is a
 * {@link CodedError} so callers can do
 * {@code err instanceof CodedError && err.code === '...'} for actionable
 * advice. Otherwise a plain {@link Error} is returned.
 *
 * The text/html fallback is run through the same PII deny-list as
 * {@link sanitizeStoredError}, plus a length cap (120 chars) so a giant
 * stack trace cannot reach the UI.
 */
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
      return new CodedError(message || fallback, code)
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

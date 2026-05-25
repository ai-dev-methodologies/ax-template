/*
---
template_id: L4/email-outbox/app/parse-error
layer: L4
domain: email-outbox
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 email-outbox vertical — shared RFC 9457 ProblemDetail unwrap + sanitizeStoredError helper. R50 stored-server-error-sanitize-at-render-layer applied to EmailOutbox.lastError before rendering."
  - source_type: external
    citation: "RFC 9457 — Problem Details for HTTP APIs"
    url: "https://datatracker.ietf.org/doc/html/rfc9457"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/

export async function parseError(res: Response, fallback: string): Promise<Error> {
  const cloned = res.clone()
  try {
    const body = await res.json()
    if (body?.detail) return new Error(String(body.detail))
    if (body?.message) return new Error(String(body.message))
  } catch {
    /* fall through */
  }
  try {
    const raw = (await cloned.text()).trim()
    const stripped = raw.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
    const looksSensitive = SENSITIVE_RE.some((re) => re.test(stripped))
    if (stripped.length > 0 && stripped.length <= 120 && !looksSensitive) {
      return new Error(stripped)
    }
  } catch {
    /* fall through */
  }
  return new Error(`${fallback} (HTTP ${res.status})`)
}

// R50 stored-server-error-sanitize-at-render-layer: applied at the
// outbox row render site to EmailOutbox.lastError before inline display.
const STORED_ERROR_MAX = 200
const SENSITIVE_RE = [
  /@[\w.-]+\.[A-Za-z]{2,}/,
  /\b(?:sk-|pk-|Bearer\s+|jdbc:|-----BEGIN |ghp_|ghs_)/i,
  /\b\d{1,3}(?:\.\d{1,3}){3}\b/,
  /\.internal\b|\.local\b/,
  /\d{6}-\d{7}/,
  /01[016789]-?\d{3,4}-?\d{4}/,
  /eyJ[A-Za-z0-9._-]{20,}/,
]
export function sanitizeStoredError(raw: string | null): string {
  if (!raw) return ''
  const sensitive = SENSITIVE_RE.some((re) => re.test(raw))
  if (sensitive) return '[redacted — see server logs]'
  return raw.length <= STORED_ERROR_MAX ? raw : `${raw.slice(0, STORED_ERROR_MAX)}… [truncated]`
}

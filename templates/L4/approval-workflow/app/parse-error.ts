/*
---
template_id: L4/approval-workflow/app/parse-error
layer: L4
domain: approval-workflow
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 approval-workflow vertical — shared ProblemDetail unwrap. Extracted to a single module (R43 iter4 / P1-iter3-N5) so /new and /[id] cannot drift on how server errors are surfaced."
  - source_type: external
    citation: "RFC 9457 — Problem Details for HTTP APIs"
    url: "https://datatracker.ietf.org/doc/html/rfc9457"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/

/**
 * parseError — convert a non-ok Response into an Error carrying the
 * server's user-facing detail (RFC 9457 ProblemDetail body).
 *
 * Fork-receivers whose server emits a different error shape should
 * extend this function — not branch the call-sites. Keeping one
 * implementation per template prevents the "/new shows useful errors,
 * /[id] shows HTTP 400" asymmetry that R43 iter2 surfaced.
 */
export async function parseError(res: Response, fallback: string): Promise<Error> {
  // Clone so a downstream caller could still inspect the body if needed.
  const cloned = res.clone()
  try {
    const body = await res.json()
    if (body?.detail) return new Error(String(body.detail))
    if (body?.message) return new Error(String(body.message))
  } catch {
    /* JSON parse failed — try text/* body next */
  }
  // R43 iter5 (P1-iter4-N3) + iter6 (P1-iter5-N3): Spring behind an L7
  // proxy commonly emits text/html error pages on 502/504. If JSON failed
  // but the body has a short human-readable message, surface that — but
  // strip HTML tags so the user does not see '<html><body>...' raw.
  try {
    const raw = (await cloned.text()).trim()
    // Strip tags + collapse whitespace; covers Tomcat / nginx / Cloudflare
    // default error pages without dragging in a full HTML sanitizer.
    const stripped = raw.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
    if (stripped.length > 0 && stripped.length <= 240) {
      return new Error(stripped)
    }
  } catch {
    /* fall through */
  }
  return new Error(`${fallback} (HTTP ${res.status})`)
}

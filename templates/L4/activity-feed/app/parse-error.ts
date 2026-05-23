/*
---
template_id: L4/activity-feed/app/parse-error
layer: L4
domain: activity-feed
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 activity-feed vertical — shared ProblemDetail unwrap. Mirrors L4/approval-workflow's parse-error.ts (R43 closure) so the catalog teaches one error-shape contract across L4s."
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
    /* JSON parse failed — try text/* body next */
  }
  // text/html fallback (Cloudflare / nginx / Tomcat default error pages)
  // R44 iter2 (P2-F5): deny-list patterns that commonly carry PII or
  // secrets in server-emitted error bodies — surfacing them verbatim
  // would leak into the user-visible error banner. On match, fall
  // through to the generic fallback. Length ceiling tightened 240→120.
  try {
    const raw = (await cloned.text()).trim()
    const stripped = raw.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
    const looksSensitive =
      /@[\w.-]+\.[A-Za-z]{2,}/.test(stripped) || // email
      /\b(?:sk-|pk-|Bearer\s+|jdbc:|-----BEGIN )/i.test(stripped) || // tokens / connection strings / PEM
      /\b\d{1,3}(?:\.\d{1,3}){3}\b/.test(stripped) || // IPv4 (private hosts)
      /\.internal\b|\.local\b/.test(stripped) // internal hostnames
    if (stripped.length > 0 && stripped.length <= 120 && !looksSensitive) {
      return new Error(stripped)
    }
  } catch {
    /* fall through */
  }
  return new Error(`${fallback} (HTTP ${res.status})`)
}

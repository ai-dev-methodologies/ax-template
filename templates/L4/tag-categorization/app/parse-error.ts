/*
---
template_id: L4/tag-categorization/app/parse-error
layer: L4
domain: tag-categorization
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 tag-categorization vertical — shared RFC 9457 ProblemDetail unwrap with text/html fallback + PII deny-list (R44 closure)."
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
  try {
    const raw = (await cloned.text()).trim()
    const stripped = raw.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
    // R44 lesson (P2-F5): PII / secret deny-list before surfacing.
    const looksSensitive =
      /@[\w.-]+\.[A-Za-z]{2,}/.test(stripped) ||
      /\b(?:sk-|pk-|Bearer\s+|jdbc:|-----BEGIN )/i.test(stripped) ||
      /\b\d{1,3}(?:\.\d{1,3}){3}\b/.test(stripped) ||
      /\.internal\b|\.local\b/.test(stripped)
    if (stripped.length > 0 && stripped.length <= 120 && !looksSensitive) {
      return new Error(stripped)
    }
  } catch {
    /* fall through */
  }
  return new Error(`${fallback} (HTTP ${res.status})`)
}

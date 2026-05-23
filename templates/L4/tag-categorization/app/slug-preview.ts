/*
---
template_id: L4/tag-categorization/app/slug-preview
layer: L4
domain: tag-categorization
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 tag-categorization vertical — client-side slug preview that mirrors backend TagSlugger (NFKD normalize → ASCII filter → hyphenate, Korean fallback when ASCII slugger empties the input). Preview-only; backend is source of truth."
  - source_type: external
    citation: "Unicode Normalization Forms — NFKD"
    url: "https://www.unicode.org/reports/tr15/"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/

/**
 * previewSlug — client-side approximation of the backend TagSlugger.
 *
 * Backend invariant (R32 / TagSlugger.java): NFKD normalize, strip
 * combining marks, keep [a-z0-9-] only, collapse runs of '-', and
 * fall back to a Korean-prefix slug when the ASCII result is empty
 * (so 한글-only tag names get a stable non-empty slug).
 *
 * This function is PREVIEW ONLY. The server is the source of truth
 * for the slug a Tag actually gets — the UI calls this so the user
 * can see what slug their typed name will produce, but on submit the
 * server runs its own canonical slugger and the response's `slug`
 * field is authoritative. Drift between this preview and the server
 * is acceptable for the catalog baseline; fork-receivers that need
 * exact parity should call a backend `POST /api/tags/preview-slug`
 * (a future contract extension).
 */
export function previewSlug(name: string): string {
  if (!name) return ''
  const trimmed = name.trim()
  if (!trimmed) return ''
  // NFKD normalize and strip combining marks (\p{M} requires unicode flag).
  const decomposed = trimmed.normalize('NFKD').replace(/\p{M}/gu, '')
  const ascii = decomposed
    .toLowerCase()
    .replace(/[^a-z0-9-]+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')
  // R45 iter2 (F1 high): no client-side Korean (or other non-ASCII)
  // fallback. The backend TagSlugger emits a `tag-<random>` slug for
  // any input whose ASCII slug is empty; we cannot reproduce that
  // randomness on the client without a round-trip. Returning '' lets
  // the "(server will pick a slug)" message do the truth-telling, and
  // removes the false-confidence case where the preview showed
  // 'tag-긴급' but the server actually wrote 'tag-a3f1'. Fork-receivers
  // who want exact parity should add `POST /api/tags/preview-slug`.
  return ascii
}

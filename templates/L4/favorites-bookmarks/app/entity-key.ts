/*
---
template_id: L4/favorites-bookmarks/app/entity-key
layer: L4
domain: favorites-bookmarks
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 favorites-bookmarks vertical — entityType/entityId validation guard. Prevents path-segment injection (entityType containing '/', '?', '#', or '..') from reaching the backend even when encodeURIComponent would have masked it on the wire."
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API1:2023 BOLA defense-in-depth"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/

/**
 * R46 iter2 (F6 medium): defense-in-depth path-segment validation.
 *
 * `encodeURIComponent` correctly escapes path-injection characters on
 * the wire, but Spring MVC's default URL handling decodes them back
 * before PathVariable matching. The backend SHOULD constrain
 * entityType/entityId charset (it currently only enforces maxLength).
 * This client-side guard rejects values that look like traversal
 * attempts so the request never leaves the browser.
 *
 * Rule: reject anything containing `/`, `?`, `#`, `\0`, or a leading
 * `.` (which combined with `.` becomes `..`). Maxlength 255 mirrors
 * the backend AddFavoriteRequest constraint.
 */
export function assertSafeEntityRef(entityType: string, entityId: string): void {
  for (const [name, value, max] of [
    ['entityType', entityType, 64],
    ['entityId', entityId, 255],
  ] as const) {
    if (!value || value.length === 0) {
      throw new Error(`Invalid ${name}: empty`)
    }
    if (value.length > max) {
      throw new Error(`Invalid ${name}: longer than ${max} characters`)
    }
    if (/[\\/?#\0]/.test(value)) {
      throw new Error(`Invalid ${name}: contains forbidden characters`)
    }
    if (value.startsWith('.')) {
      throw new Error(`Invalid ${name}: cannot start with '.'`)
    }
  }
}

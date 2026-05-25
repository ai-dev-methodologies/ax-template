/*
---
template_id: L0/fork-receiver-kit/entity-key
layer: L0
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API1:2023 BOLA defense-in-depth"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
  - source_type: internal
    rationale: "R53 fork-receiver-kit consolidation — path-segment guard previously lived only in templates/L4/favorites-bookmarks/app/entity-key.ts (R46 iter2 F6 closure). Promoting to L0 so every L4 with polymorphic (entityType, entityId) URLs picks up the same defense-in-depth check instead of duplicating it (or, more dangerously, skipping it)."
imports_from: []
imports_forbidden: [L1, L2, L3, L4, app/, lib/]
---
*/

/**
 * Defense-in-depth path-segment validation for polymorphic entity references.
 *
 * <p>{@code encodeURIComponent} correctly escapes path-injection characters
 * on the wire, but Spring MVC's default URL handling decodes them back
 * before {@code @PathVariable} matching. Backends SHOULD constrain
 * entityType/entityId charset (most catalog backends enforce maxLength but
 * not character class). This client-side guard rejects values that look
 * like traversal attempts so the request never leaves the browser.
 *
 * <h2>Rule</h2>
 * Reject anything containing {@code /}, {@code ?}, {@code #}, {@code \0},
 * or starting with {@code .} (which combined with another {@code .} becomes
 * {@code ..}). Maxlength 64 (entityType) and 255 (entityId) mirror the
 * canonical AddFavoriteRequest / Comment / Activity / Tag attachment
 * constraints — fork-receivers with different caps can override by calling
 * with their own values (see {@link MaxLengths}).
 */
export interface MaxLengths {
  entityType: number
  entityId: number
}

const DEFAULTS: MaxLengths = { entityType: 64, entityId: 255 }

export function assertSafeEntityRef(
  entityType: string,
  entityId: string,
  maxLengths: MaxLengths = DEFAULTS,
): void {
  for (const [name, value, max] of [
    ['entityType', entityType, maxLengths.entityType],
    ['entityId', entityId, maxLengths.entityId],
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

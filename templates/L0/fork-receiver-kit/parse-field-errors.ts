/*
---
template_id: L0/fork-receiver-kit/parse-field-errors
layer: L0
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "RFC 9457 — Problem Details for HTTP APIs, §3.1 (extension members) — the 'invalid-params' / 'errors' array convention for field-level validation detail"
    url: "https://datatracker.ietf.org/doc/html/rfc9457"
  - source_type: external
    citation: "Jakarta Bean Validation 3.0 — ConstraintViolation.getPropertyPath() (the 'violations' shape Spring emits for @Valid failures)"
    url: "https://jakarta.ee/specifications/bean-validation/3.0/"
  - source_type: internal
    rationale: "FDW1 (frontend dogfood) rule-of-three: all 3 personas hand-rolled the SAME glue mapping a server ProblemDetail's field-level validation array (and single-field CodedError codes) into a Record<fieldName,message> for react-hook-form setError / a form-error-summary. parse-error.ts returns ONE flat Error and stops at the message; the field seam had no catalog primitive. This sibling closes the #1 completeness gap so crud-create-form / crud-edit-form can satisfy CRUD-FE-006 ('validated form maps server problem+json to fields')."
imports_from: []
imports_forbidden: [L1, L2, L3, L4, app/, lib/]
---
*/

/**
 * parse-field-errors — map a backend RFC 9457 ProblemDetail's field-level
 * validation detail into a flat `Record<fieldName, message>` the form layer
 * can hand to react-hook-form's `setError` (or a form-error-summary block).
 *
 * Pairs with {@link ../parse-error} (which extracts the top-level message +
 * preserves a `code` via CodedError). This module handles the *per-field* seam.
 *
 * The catalog refuses to claim ownership of a field-error wire shape, so the
 * extractor is shape-tolerant: it accepts every common Spring/Jakarta/RFC 9457
 * convention seen across the dogfood and normalises them.
 */

/** Read a string property off an unknown object without throwing. */
function str(obj: unknown, ...keys: string[]): string | undefined {
  if (!obj || typeof obj !== 'object') return undefined
  for (const k of keys) {
    const v = (obj as Record<string, unknown>)[k]
    if (typeof v === 'string' && v.length > 0) return v
  }
  return undefined
}

/** Pull the array of per-field entries from whichever member carries it. */
function fieldEntryArray(body: unknown): unknown[] {
  if (!body || typeof body !== 'object') return []
  const b = body as Record<string, unknown>
  // RFC 9457 example uses "invalid-params" ({name, reason}); Spring/Jakarta use
  // "errors" / "violations" / "fieldErrors"; "invalidParams" is a camelCase variant.
  const candidates = [
    b['invalid-params'],
    b.invalidParams,
    b.violations,
    b.errors,
    b.fieldErrors,
  ]
  for (const c of candidates) {
    if (Array.isArray(c)) return c
  }
  return []
}

/**
 * extractFieldErrors — pure transform of an already-parsed ProblemDetail body.
 *
 * - Per-field array entries: field key is read from
 *   `field` / `propertyPath` / `property` / `name` / `pointer` (first present);
 *   message from `message` / `defaultMessage` / `reason` / `detail`.
 * - A top-level `code` string is mapped through the optional `codeToField`
 *   table (e.g. `{ EMAIL_TAKEN: 'email' }`) so a single-field coded 400 also
 *   lands on the right input.
 *
 * Later entries do NOT overwrite an earlier message for the same field (first
 * message wins — the order the server reported them).
 */
export function extractFieldErrors(
  body: unknown,
  codeToField?: Record<string, string>,
): Record<string, string> {
  const out: Record<string, string> = {}

  for (const entry of fieldEntryArray(body)) {
    const field = str(entry, 'field', 'propertyPath', 'property', 'name', 'pointer')
    const message = str(entry, 'message', 'defaultMessage', 'reason', 'detail')
    if (field && message && !(field in out)) {
      out[field] = message
    }
  }

  // Single-field coded error (no array) → map the code to a field if known.
  if (codeToField) {
    const code = str(body, 'code')
    const detail = str(body, 'detail', 'message')
    if (code && codeToField[code] && !(codeToField[code] in out)) {
      out[codeToField[code]] = detail || code
    }
  }

  return out
}

/**
 * parseFieldErrors — extract field errors from a failed `fetch` Response.
 * Returns `{}` when the body is not JSON or carries no field detail (callers
 * then fall back to {@link ../parse-error} for the top-level message).
 */
export async function parseFieldErrors(
  res: Response,
  codeToField?: Record<string, string>,
): Promise<Record<string, string>> {
  try {
    const body = await res.clone().json()
    return extractFieldErrors(body, codeToField)
  } catch {
    return {}
  }
}

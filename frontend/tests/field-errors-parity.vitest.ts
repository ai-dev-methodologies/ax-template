import { readFileSync } from 'fs'
import { join } from 'path'
import { describe, it, expect } from 'vitest'

// BACKLOG P2-36 — FE leg of the validation `errors[]` contract-parity pair.
//
// Reads the SAME committed golden as
// backend/src/test/java/.../common/ValidationErrorsContractParityTest.java
// (frontend/tests/_fixtures/validation-error.golden.json) — one source of
// truth, two independent consumers, both READ-ONLY. The golden is produced by
// the real GlobalProblemDetailAdvice emission path; regeneration is a separate
// explicit gradle command (-Dgolden.regenerate=true), never part of either
// assertion path.
//
// Before this pair existed, the only exercise of extractFieldErrors was the
// hand-built `{field, defaultMessage}` object in fmw2-primitives.vitest.ts —
// a shape the backend never emits, retained there only as documented
// legacy-shape tolerance. This file pins the shape the backend REALLY sends.

import { extractFieldErrors, parseFieldErrors } from '../../templates/L0/fork-receiver-kit/parse-field-errors'

type GoldenEntry = Record<string, string>
type Golden = Record<string, unknown> & { errors: GoldenEntry[] }

function readGolden(): Golden {
  const raw = readFileSync(join(process.cwd(), 'tests/_fixtures/validation-error.golden.json'), 'utf8')
  return JSON.parse(raw) as Golden
}

function problemJsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/problem+json' },
  })
}

describe('parse-field-errors — parity with the BE-emitted validation errors[] golden (P2-36)', () => {
  it('maps every BE-emitted errors[] entry onto its `field` key with the BE `message`', () => {
    const golden = readGolden()

    const fields = extractFieldErrors(golden)

    // Keyed by the entry's `field` member. NOTE: the golden's `name` is an
    // identical mirror of `field` (and `detail` mirrors `message`), so this
    // assertion alone CANNOT prove `field`/`message` resolve ahead of their
    // sibling aliases — dropping either branch falls through to the
    // identical-valued sibling and this stays green either way. That
    // per-branch isolation lives in the "branch isolation" describe block
    // below, which uses synthetic single-alias entries. This assertion
    // instead pins the exact shape the real backend advice emits end-to-end.
    expect(fields).toEqual({
      email: 'must not be blank',
      'profile.age': 'must be greater than or equal to 0',
      createItemRequest: 'password and confirmation must match',
    })

    // Non-vacuity: the golden really is the multi-entry BE shape, not a stub.
    expect(golden.errors).toHaveLength(3)
    expect(Object.keys(fields)).toHaveLength(golden.errors.length)
  })

  it('reads the same field/message members the BE promises per entry', () => {
    const golden = readGolden()

    for (const entry of golden.errors) {
      expect(Object.keys(entry).sort()).toEqual(
        ['code', 'detail', 'field', 'message', 'name', 'pointer'],
      )
      // The two members the parser actually dereferences must be present and
      // non-empty on every entry — the contract this leg locks.
      expect(entry.field.length).toBeGreaterThan(0)
      expect(entry.message.length).toBeGreaterThan(0)
    }
  })

  it('falls back to the RFC 6901 pointer when `field`/`name` are absent', () => {
    const golden = readGolden()
    // Prove the documented fallback chain against the REAL pointer values the
    // advice emits (dotted java path -> '/profile/age'), not an invented shape.
    const pointerOnly = {
      ...golden,
      errors: golden.errors.map(({ field, name, ...rest }) => {
        void field
        void name
        return rest
      }),
    }

    expect(extractFieldErrors(pointerOnly)).toEqual({
      '/email': 'must not be blank',
      '/profile/age': 'must be greater than or equal to 0',
      '/createItemRequest': 'password and confirmation must match',
    })
  })

  it('extracts the same map from a real problem+json Response body', async () => {
    const golden = readGolden()

    const fields = await parseFieldErrors(
      problemJsonResponse(golden, golden.status as number),
    )

    expect(fields).toEqual({
      email: 'must not be blank',
      'profile.age': 'must be greater than or equal to 0',
      createItemRequest: 'password and confirmation must match',
    })
  })
})

describe('parse-field-errors — branch isolation (synthetic single-alias inputs, NOT server golden)', () => {
  // The golden above carries every alias in parallel on every entry
  // (`field`===`name`, `message`===`detail`), so an assertion against it
  // cannot tell WHICH alias actually supplied a value — deleting a
  // resolution branch just falls through to an identical-valued sibling key
  // and the assertion stays green (P1-1, codex gpt-5.6-sol xhigh).
  //
  // Each entry below is a hand-built, branch-isolation-only input — it
  // deliberately carries exactly ONE field-key alias and ONE message-key
  // alias, so that deleting any single branch from parse-field-errors.ts's
  // alias lists (line 82 field-key list, line 83 message-key list) drops
  // that entry from the result entirely and turns the assertion red.
  //   field-key order:   field > propertyPath > property > name > pointer
  //   message-key order: message > defaultMessage > reason > detail
  //
  // The golden fixture keeps its role as the untouched fidelity anchor;
  // nothing here is derived from or written back into it.

  it('field-key: resolves via `field`, and message-key via `message` (both first aliases)', () => {
    expect(extractFieldErrors({ errors: [{ field: 'x', message: 'm' }] })).toEqual({ x: 'm' })
  })

  it('field-key: falls back to `propertyPath` when `field` is absent', () => {
    expect(extractFieldErrors({ errors: [{ propertyPath: 'x', message: 'm' }] })).toEqual({ x: 'm' })
  })

  it('field-key: falls back to `property` when `field`/`propertyPath` are absent', () => {
    expect(extractFieldErrors({ errors: [{ property: 'x', message: 'm' }] })).toEqual({ x: 'm' })
  })

  it('field-key: falls back to `name` when `field`/`propertyPath`/`property` are absent', () => {
    expect(extractFieldErrors({ errors: [{ name: 'x', message: 'm' }] })).toEqual({ x: 'm' })
  })

  it('field-key: falls back to `pointer` when every other field alias is absent', () => {
    expect(extractFieldErrors({ errors: [{ pointer: 'x', message: 'm' }] })).toEqual({ x: 'm' })
  })

  it('message-key: falls back to `defaultMessage` when `message` is absent', () => {
    expect(extractFieldErrors({ errors: [{ field: 'x', defaultMessage: 'm' }] })).toEqual({ x: 'm' })
  })

  it('message-key: falls back to `reason` when `message`/`defaultMessage` are absent', () => {
    expect(extractFieldErrors({ errors: [{ field: 'x', reason: 'm' }] })).toEqual({ x: 'm' })
  })

  it('message-key: falls back to `detail` when `message`/`defaultMessage`/`reason` are absent', () => {
    expect(extractFieldErrors({ errors: [{ field: 'x', detail: 'm' }] })).toEqual({ x: 'm' })
  })
})

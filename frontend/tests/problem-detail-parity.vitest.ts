import { readFileSync } from 'fs'
import { join } from 'path'
import { describe, it, expect } from 'vitest'

// S2.ERROR-CONTRACT.XB — FE leg of the FE<->BE ProblemDetail contract parity
// pair. Reads the SAME golden fixture as
// backend/src/test/java/.../common/ProblemDetailContractParityTest.java
// (frontend/tests/_fixtures/problem-detail.golden.json) — one committed
// source of truth, two independent consumers. A drift in either the
// GlobalProblemDetailAdvice-emitted shape OR this parser trips exactly one
// of the two tests, never silently. Planted for exactly this gap by
// CANARY-013.

import { parseError, CodedError } from '../../templates/L0/fork-receiver-kit/parse-error'

function readGolden(): Record<string, unknown> {
  const raw = readFileSync(join(process.cwd(), 'tests/_fixtures/problem-detail.golden.json'), 'utf8')
  return JSON.parse(raw) as Record<string, unknown>
}

function problemJsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/problem+json' },
  })
}

describe('parseError — parity with the BE-emitted ProblemDetail golden (S2.ERROR-CONTRACT.XB)', () => {
  it('reads detail as the message and preserves code as a CodedError from the real BE-shaped golden fixture', async () => {
    const golden = readGolden()
    const res = problemJsonResponse(golden, golden.status as number)

    const err = await parseError(res, 'fallback message')

    // parseError() resolution order is body.detail -> body.message -> ...;
    // GlobalProblemDetailAdvice always populates `detail` (never `message`),
    // so the golden's detail must be exactly what a caller sees.
    expect(err.message).toBe(golden.detail)
    expect(err).toBeInstanceOf(CodedError)
    expect((err as CodedError).code).toBe(golden.code)
  })

  it('does not depend on type/title/instance — parseError never reads them', async () => {
    const golden = readGolden()
    // Strip the three RFC 9457 members parse-error.ts's documented resolution
    // order (detail -> message -> text/html fallback) never dereferences, to
    // prove the parser's real dependency set is exactly {detail, code} (plus
    // the `message` fallback it never gets to exercise here).
    const { type, title, instance, ...withoutOptionalMembers } = golden
    void type
    void title
    void instance
    const res = problemJsonResponse(withoutOptionalMembers, golden.status as number)

    const err = await parseError(res, 'fallback message')

    expect(err.message).toBe(golden.detail)
    expect((err as CodedError).code).toBe(golden.code)
  })
})

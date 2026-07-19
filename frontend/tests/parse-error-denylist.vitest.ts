import { describe, it, expect } from 'vitest'
// S2.AUDIT-PII.FE dogfood closure — parse-error.ts's PII deny-list
// (sanitizeStoredError) existed but was completely UNTESTED (0 hits across
// frontend/tests/*.vitest.ts before this file) and was only PARTIALLY wired:
// it ran on the text/html fallback branch of parseError() but NOT on the
// JSON body.detail / body.message branch — the branch every RFC 9457
// ProblemDetail actually takes. A backend that (very plausibly) echoes a
// submitted RRN/email/phone into `detail` for a validation error leaked it
// to the browser verbatim. See templates/L0/fork-receiver-kit/parse-error.ts.
import { parseError, sanitizeStoredError, CodedError } from '../../templates/L0/fork-receiver-kit/parse-error'

function jsonResponse(body: unknown, status = 400): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })
}

describe('sanitizeStoredError — deny-list redacts each PII shape', () => {
  it('redacts a KR resident registration number (RRN)', () => {
    expect(sanitizeStoredError('rrn 900101-1234567 on file')).toBe('rrn [REDACTED] on file')
  })

  it('redacts a KR mobile number', () => {
    expect(sanitizeStoredError('call 010-1234-5678 now')).toBe('call [REDACTED] now')
  })

  it('redacts an email address', () => {
    expect(sanitizeStoredError('contact user@example.com')).toBe('contact [REDACTED]')
  })

  it('redacts a JWT-shaped token', () => {
    expect(sanitizeStoredError('token eyJhbGciOiJIUzI1NiJ9.payloadpayload.sig')).toBe('token [REDACTED]')
  })

  it('redacts a Bearer auth header value', () => {
    expect(sanitizeStoredError('Authorization: Bearer abc123.def456')).toBe('Authorization: [REDACTED]')
  })

  it('passes through null/undefined as empty string without throwing', () => {
    expect(sanitizeStoredError(null)).toBe('')
    expect(sanitizeStoredError(undefined)).toBe('')
  })
})

describe('parseError — JSON ProblemDetail branch (the untested seam)', () => {
  it('does NOT leak an RRN echoed into a validation error detail', async () => {
    const res = jsonResponse({ detail: 'invalid resident number 900101-1234567 for applicant' })
    const err = await parseError(res, 'Request failed')
    expect(err.message).not.toMatch(/\d{6}-\d{7}/)
  })

  it('does NOT leak an email echoed into body.message', async () => {
    const res = jsonResponse({ message: 'duplicate account for user@example.com' })
    const err = await parseError(res, 'Request failed')
    expect(err.message).not.toMatch(/[\w.-]+@[\w.-]+\.[A-Za-z]{2,}/)
  })

  it('still preserves body.code on a CodedError after sanitizing detail', async () => {
    const res = jsonResponse({ detail: 'rrn 900101-1234567 already registered', code: 'DUPLICATE_RRN' })
    const err = await parseError(res, 'Request failed')
    expect(err).toBeInstanceOf(CodedError)
    expect((err as CodedError).code).toBe('DUPLICATE_RRN')
    expect(err.message).not.toMatch(/\d{6}-\d{7}/)
  })

  it('leaves an ordinary non-PII detail message untouched', async () => {
    const res = jsonResponse({ detail: 'quota exceeded, try again later' })
    const err = await parseError(res, 'Request failed')
    expect(err.message).toBe('quota exceeded, try again later')
  })
})

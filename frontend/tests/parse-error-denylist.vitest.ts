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

// A non-JSON (text/html) body: res.json() throws, so parseError() takes the
// text/html fallback branch (parse-error.ts ~line 112+), which strips tags,
// screens for PII via `looksSensitive`, and caps length at 120 chars.
function textResponse(body: string, status = 500): Response {
  return new Response(body, {
    status,
    headers: { 'content-type': 'text/html' },
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

// The text/html fallback branch (parse-error.ts's `looksSensitive` screen).
// This is the branch codex flagged as UNTESTED — the JSON tests above never
// reach it. These cases make the branch RED-able: reverting the looksSensitive
// protection (so short stripped text is returned unconditionally) leaks the PII
// bodies below verbatim → these assertions FAIL. Each PII body is kept < 120
// chars stripped, so it is the PII screen — not the length cap — that blocks it.
describe('parseError — text/html fallback branch (the previously-untested seam)', () => {
  it('does NOT leak an RRN from a non-JSON (text/html) error body', async () => {
    const res = textResponse('<p>invalid rrn 900101-1234567</p>')
    const err = await parseError(res, 'Request failed')
    expect(err.message).not.toMatch(/\d{6}-\d{7}/)
    expect(err.message).toBe('Request failed (HTTP 500)')
  })

  it('does NOT leak an email from a non-JSON (text/html) error body', async () => {
    const res = textResponse('<div>duplicate for user@example.com</div>')
    const err = await parseError(res, 'Request failed')
    expect(err.message).not.toMatch(/[\w.-]+@[\w.-]+\.[A-Za-z]{2,}/)
    expect(err.message).toBe('Request failed (HTTP 500)')
  })

  it('does NOT leak a Bearer/JWT token from a non-JSON (text/html) error body', async () => {
    const res = textResponse('<pre>token eyJhbGciOiJIUzI1NiJ9.payloadpayload.sigsig</pre>')
    const err = await parseError(res, 'Request failed')
    expect(err.message).not.toMatch(/eyJ[A-Za-z0-9._-]{20,}/)
    expect(err.message).toBe('Request failed (HTTP 500)')
  })

  it('does NOT leak an internal hostname/IP from a non-JSON (text/html) error body', async () => {
    const res = textResponse('<p>connect failed db-01.internal 10.2.3.4</p>')
    const err = await parseError(res, 'Request failed')
    expect(err.message).not.toMatch(/\.internal\b/)
    expect(err.message).not.toMatch(/\b(?:\d{1,3}\.){3}\d{1,3}\b/)
    expect(err.message).toBe('Request failed (HTTP 500)')
  })

  it('surfaces a short, non-PII text/html body (branch genuinely reached, not always-fallback)', async () => {
    const res = textResponse('<p>Service temporarily unavailable</p>', 503)
    const err = await parseError(res, 'Request failed')
    expect(err.message).toBe('Service temporarily unavailable')
  })
})

import { describe, it, expect } from 'vitest'
// FMW2 catalog primitives live at repo-root templates/ (copied into a fork's
// src/ on adoption). We test the two PURE ones here for logic correctness —
// the rounding + shape-tolerance are the real risk surfaces.
import { toMinorUnits, toMajorUnits } from '../../templates/L0/fork-receiver-kit/money'
import {
  extractFieldErrors,
} from '../../templates/L0/fork-receiver-kit/parse-field-errors'

describe('money — integer minor units, no float', () => {
  it('parses simple decimals exactly (avoids the 19.99*100 float bug)', () => {
    expect(toMinorUnits('19.99')).toBe(1999n)
    expect(toMinorUnits('0.01')).toBe(1n)
    expect(toMinorUnits('100')).toBe(10000n)
    expect(toMinorUnits('0')).toBe(0n)
  })

  it('pads short fractions and rounds half-up on excess digits', () => {
    expect(toMinorUnits('1.5')).toBe(150n)
    expect(toMinorUnits('1.005')).toBe(101n) // 1.005 -> 100.5 -> half-up 101
    expect(toMinorUnits('1.004')).toBe(100n)
    expect(toMinorUnits('2.995')).toBe(300n)
  })

  it('handles negatives and number inputs', () => {
    expect(toMinorUnits('-3.5')).toBe(-350n)
    expect(toMinorUnits(19.99)).toBe(1999n)
  })

  it('honours a custom fraction-digit width', () => {
    expect(toMinorUnits('1.2345', 4)).toBe(12345n)
    expect(toMinorUnits('100', 0)).toBe(100n)
  })

  it('throws on a non-decimal amount rather than producing NaN', () => {
    expect(() => toMinorUnits('abc')).toThrow(RangeError)
    expect(() => toMinorUnits('')).toThrow(RangeError)
  })

  it('round-trips through toMajorUnits', () => {
    expect(toMajorUnits(1999n)).toBe('19.99')
    expect(toMajorUnits(1n)).toBe('0.01')
    expect(toMajorUnits(-350n)).toBe('-3.50')
    expect(toMajorUnits(100n, 0)).toBe('100')
    expect(toMajorUnits(toMinorUnits('1234.56'))).toBe('1234.56')
  })
})

describe('parse-field-errors — shape-tolerant ProblemDetail field extraction', () => {
  it('reads Spring/Jakarta "violations" ({ propertyPath, message })', () => {
    const body = {
      violations: [
        { propertyPath: 'email', message: 'must be a valid email' },
        { propertyPath: 'age', message: 'must be >= 0' },
      ],
    }
    expect(extractFieldErrors(body)).toEqual({
      email: 'must be a valid email',
      age: 'must be >= 0',
    })
  })

  it('reads RFC 9457 "invalid-params" ({ name, reason })', () => {
    const body = { 'invalid-params': [{ name: 'price', reason: 'must be positive' }] }
    expect(extractFieldErrors(body)).toEqual({ price: 'must be positive' })
  })

  // LEGACY-SHAPE TOLERANCE ONLY (BACKLOG P2-36). `{field, defaultMessage}` is the
  // raw Spring BindingResult shape — this repo's GlobalProblemDetailAdvice does NOT
  // emit it (it emits {field,name,pointer,code,message,detail}). Kept because
  // fork-receivers on an older/plainer advice still send it. The real BE<->FE parity
  // is pinned by tests/field-errors-parity.vitest.ts against the committed golden.
  it('reads Spring "errors" ({ field, defaultMessage })', () => {
    const body = { errors: [{ field: 'title', defaultMessage: 'must not be blank' }] }
    expect(extractFieldErrors(body)).toEqual({ title: 'must not be blank' })
  })

  it('first message wins for a repeated field', () => {
    const body = {
      errors: [
        { field: 'name', message: 'too short' },
        { field: 'name', message: 'also bad' },
      ],
    }
    expect(extractFieldErrors(body)).toEqual({ name: 'too short' })
  })

  it('maps a single-field code through codeToField', () => {
    const body = { code: 'EMAIL_TAKEN', detail: 'that email is registered' }
    expect(extractFieldErrors(body, { EMAIL_TAKEN: 'email' })).toEqual({
      email: 'that email is registered',
    })
  })

  it('returns {} for a body with no field detail', () => {
    expect(extractFieldErrors({ detail: 'generic 400' })).toEqual({})
    expect(extractFieldErrors(null)).toEqual({})
    expect(extractFieldErrors('not an object')).toEqual({})
  })
})

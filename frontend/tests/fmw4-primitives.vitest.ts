import { describe, it, expect } from 'vitest'

// NOTE: use-url-list-state.ts statically imports next/navigation; vitest.config
// aliases that specifier to tests/_stubs/next-navigation.ts so this out-of-root
// template module resolves. listStateToQuery (the export under test) never calls
// the hooks, so the inert stub is sufficient.

// FMW4 catalog primitives live at repo-root templates/ (copied into a fork's
// src/ on adoption). We test the PURE additions here — the wire-type coercion,
// the inverse list-state serializer, and the conflict classifier — since those
// are the real logic-risk surfaces (the hooks are exercised by component tests).
import {
  parseMinor,
  serializeMinor,
  fractionDigitsFor,
  toMinorUnits,
} from '../../templates/L0/fork-receiver-kit/money'
import {
  listStateToQuery,
  type ListState,
} from '../../templates/L0/fork-receiver-kit/use-url-list-state'
import { classifyConflict } from '../../templates/L0/fork-receiver-kit/use-conflict-resolution'

// Mirror the form-layer conversion: toMinorUnits sized by the currency's width.
function toMinor(amount: string, currency: string): bigint {
  return toMinorUnits(amount, fractionDigitsFor(currency))
}

describe('money wire-type — parseMinor / serializeMinor / fractionDigitsFor (FMW4d)', () => {
  it('parseMinor lifts a JSON number/string into a bigint', () => {
    expect(parseMinor(1999)).toBe(1999n)
    expect(parseMinor('1999')).toBe(1999n)
    expect(parseMinor('-50')).toBe(-50n)
    expect(parseMinor(0)).toBe(0n)
    expect(parseMinor(9007199254740993n)).toBe(9007199254740993n) // bigint pass-through
  })

  it('parseMinor throws on a non-integer (leaked major units) instead of truncating', () => {
    expect(() => parseMinor(19.99)).toThrow(RangeError)
    expect(() => parseMinor('19.99')).toThrow(RangeError)
    expect(() => parseMinor('abc')).toThrow(RangeError)
    expect(() => parseMinor('')).toThrow(RangeError)
  })

  it('serializeMinor picks a number under MAX_SAFE_INTEGER and a string beyond it', () => {
    expect(serializeMinor(1999n)).toBe(1999)
    expect(serializeMinor(0n)).toBe(0)
    expect(serializeMinor(-350n)).toBe(-350)
    expect(serializeMinor(BigInt(Number.MAX_SAFE_INTEGER))).toBe(Number.MAX_SAFE_INTEGER)
    // beyond 2^53-1 → lossless decimal string (JSON.stringify(bigint) would throw)
    expect(serializeMinor(9007199254740993n)).toBe('9007199254740993')
  })

  it('parseMinor ∘ serializeMinor round-trips', () => {
    expect(parseMinor(serializeMinor(1999n))).toBe(1999n)
    expect(parseMinor(serializeMinor(9007199254740993n))).toBe(9007199254740993n)
  })

  it('fractionDigitsFor returns 0 for zero-decimal currencies, 2 otherwise', () => {
    expect(fractionDigitsFor('KRW')).toBe(0)
    expect(fractionDigitsFor('JPY')).toBe(0)
    expect(fractionDigitsFor('krw')).toBe(0) // case-insensitive
    expect(fractionDigitsFor('USD')).toBe(2)
    expect(fractionDigitsFor('EUR')).toBe(2)
  })

  it('a KRW amount sized by fractionDigitsFor does NOT gain phantom decimals', () => {
    // The bug a hard-coded 2 would cause: '1500' KRW -> 150000n instead of 1500n.
    expect(toMinor('1500', 'KRW')).toBe(1500n)
    expect(toMinor('19.99', 'USD')).toBe(1999n)
  })
})

describe('listStateToQuery — inverse serializer (FMW4d)', () => {
  const base: ListState = { page: 1, pageSize: 20, filters: {} }

  it('omits defaults (page=1, default pageSize) for clean shareable links', () => {
    expect(listStateToQuery(base)).toBe('')
  })

  it('emits page only when past page 1', () => {
    expect(listStateToQuery({ ...base, page: 3 })).toBe('page=3')
  })

  it('emits a non-default pageSize', () => {
    expect(listStateToQuery({ ...base, pageSize: 50 })).toBe('pageSize=50')
  })

  it('emits search, sort (with direction), and declared filters', () => {
    const q = listStateToQuery({
      page: 2,
      pageSize: 20,
      search: 'widget',
      sortField: 'name',
      sortDirection: 'desc',
      filters: { status: 'ACTIVE' },
    })
    expect(q).toBe('page=2&search=widget&sortField=name&sortDirection=desc&status=ACTIVE')
  })

  it('includeDefaults forces page/pageSize even at defaults', () => {
    expect(listStateToQuery(base, { includeDefaults: true })).toBe('page=1&pageSize=20')
  })

  it('respects a custom defaultPageSize when deciding to omit', () => {
    expect(listStateToQuery({ ...base, pageSize: 10 }, { defaultPageSize: 10 })).toBe('')
  })
})

describe('classifyConflict — optimistic-lock 428/412/409 → ConflictSignal (FMW4c)', () => {
  it('maps each conflict status to its kind + canonical code', () => {
    expect(classifyConflict(428, { code: 'PRECONDITION_REQUIRED' })).toMatchObject({
      kind: 'precondition-required',
      status: 428,
      code: 'PRECONDITION_REQUIRED',
    })
    expect(classifyConflict(409, { code: 'OPTIMISTIC_LOCK_CONFLICT' })).toMatchObject({
      kind: 'conflict',
      code: 'OPTIMISTIC_LOCK_CONFLICT',
    })
  })

  it('extracts the authoritative current_etag from a 412 body', () => {
    const sig = classifyConflict(412, { code: 'PRECONDITION_FAILED', current_etag: '"42-7"' })
    expect(sig).toMatchObject({ kind: 'stale', currentEtag: '"42-7"' })
  })

  it('falls back to the kind when no code member is present', () => {
    expect(classifyConflict(409, {})?.code).toBe('conflict')
    expect(classifyConflict(428, undefined)?.code).toBe('precondition-required')
  })

  it('returns null for any non-conflict status (caller uses its normal error path)', () => {
    expect(classifyConflict(400, { code: 'VALIDATION_FAILED' })).toBeNull()
    expect(classifyConflict(404, {})).toBeNull()
    expect(classifyConflict(200, {})).toBeNull()
    expect(classifyConflict(500, {})).toBeNull()
  })
})

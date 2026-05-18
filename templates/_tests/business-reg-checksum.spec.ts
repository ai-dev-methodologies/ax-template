/**
 * TDD anchor: business-reg-checksum.spec.ts
 * SP31 acceptance gate — validateBusinessRegistration() must implement the NTS checksum
 * algorithm correctly against public BRN data from 금융감독원 DART.
 *
 * RED reason: no BusinessRegistrationInput component exists yet; validateBusinessRegistration
 * is not exported.
 * GREEN: export validateBusinessRegistration + FormatViolationError from
 *   templates/L1/components/business-registration-input.tsx
 *
 * First green command:
 *   npx vitest run templates/_tests/business-reg-checksum.spec.ts
 *
 * Fixture data sources:
 *   - pass/samples.json: Real public BRNs from DART (공공저작물 자유이용허락)
 *   - fail_invalid_checksum/samples.json: Same BRNs with last digit +1 (invalid)
 *   - fail_format_violation/samples.json: Malformed inputs → FormatViolationError
 *
 * Rule: practices-react/rules/business-registration-checksum-required.md
 */

import { describe, expect, test } from 'vitest'
import { readFileSync } from 'fs'
import { join } from 'path'
import {
  validateBusinessRegistration,
  FormatViolationError,
} from '../L1/components/business-registration-input'

// ─── Fixture loading ───────────────────────────────────────────────────────

const FIXTURES_ROOT = join(
  __dirname,
  '../../practices/evals/fixtures/business-registration-checksum'
)

function loadFixture<T>(subPath: string): T {
  const filePath = join(FIXTURES_ROOT, subPath)
  return JSON.parse(readFileSync(filePath, 'utf-8')) as T
}

interface PassSample {
  brn: string
  formatted: string
  companyName: string
}

interface FailChecksumSample {
  brn: string
  formatted: string
  expected: false
}

interface FormatViolationSample {
  brn: string
  expectedError?: string
  expectedBehavior?: string
}

// ─── Tests ─────────────────────────────────────────────────────────────────

describe('validateBusinessRegistration — NTS checksum algorithm', () => {
  describe('pass: valid public BRNs (DART 공시 데이터)', () => {
    const passSamples = loadFixture<PassSample[]>('pass/samples.json')

    test.each(passSamples)(
      'validateBusinessRegistration($brn) returns true — $companyName',
      ({ brn }) => {
        expect(validateBusinessRegistration(brn)).toBe(true)
      }
    )

    test.each(passSamples)(
      'accepts formatted BRN $formatted (with hyphens) — $companyName',
      ({ formatted }) => {
        expect(validateBusinessRegistration(formatted)).toBe(true)
      }
    )
  })

  describe('fail_invalid_checksum: mutated last digit returns false', () => {
    const failSamples = loadFixture<FailChecksumSample[]>(
      'fail_invalid_checksum/samples.json'
    )

    test.each(failSamples)(
      'validateBusinessRegistration($brn) returns false (last digit mutated)',
      ({ brn, expected }) => {
        expect(validateBusinessRegistration(brn)).toBe(expected)
      }
    )

    test.each(failSamples)(
      'accepts formatted form $formatted but returns false',
      ({ formatted }) => {
        expect(validateBusinessRegistration(formatted)).toBe(false)
      }
    )
  })

  describe('fail_format_violation: throws FormatViolationError on bad input', () => {
    const formatSamples = loadFixture<FormatViolationSample[]>(
      'fail_format_violation/samples.json'
    )

    // Inputs with expectedError = FormatViolationError must throw
    const throwingSamples = formatSamples.filter(
      (s) => s.expectedError === 'FormatViolationError'
    )

    // Inputs with expectedBehavior (boundary: returns false, not throws)
    const returnFalseSamples = formatSamples.filter(
      (s) => s.expectedBehavior !== undefined
    )

    test.each(throwingSamples)(
      'validateBusinessRegistration("$brn") throws FormatViolationError',
      ({ brn }) => {
        expect(() => validateBusinessRegistration(brn)).toThrow(FormatViolationError)
      }
    )

    test.each(throwingSamples)(
      'FormatViolationError is instanceof FormatViolationError for "$brn"',
      ({ brn }) => {
        try {
          validateBusinessRegistration(brn)
          // Should not reach here
          expect.fail('Expected FormatViolationError to be thrown')
        } catch (err) {
          expect(err).toBeInstanceOf(FormatViolationError)
        }
      }
    )

    test.each(returnFalseSamples)(
      'boundary: "$brn" strips to 10 digits — checksum runs, returns false (not throws)',
      ({ brn }) => {
        // This input has 10 digits after stripping but invalid checksum
        expect(() => validateBusinessRegistration(brn)).not.toThrow()
        expect(validateBusinessRegistration(brn)).toBe(false)
      }
    )
  })

  describe('algorithm correctness — manual spot-checks', () => {
    test('Samsung Electronics 124-81-00998: sum=82, checkDigit=8', () => {
      // Verify step-by-step
      // d = [1, 2, 4, 8, 1, 0, 0, 9, 9, 8]
      // sum[0..7] = 1×1 + 2×3 + 4×7 + 8×1 + 1×3 + 0×7 + 0×1 + 9×3 = 73
      // 9th digit: floor(9×5/10) + (9×5)%10 = 4 + 5 = 9
      // total = 82; checkDigit = (10 - 2) % 10 = 8; d[9] = 8 ✓
      expect(validateBusinessRegistration('1248100998')).toBe(true)
    })

    test('FormatViolationError has correct name property', () => {
      try {
        validateBusinessRegistration('ABC')
      } catch (err) {
        expect((err as FormatViolationError).name).toBe('FormatViolationError')
      }
    })

    test('empty string throws FormatViolationError', () => {
      expect(() => validateBusinessRegistration('')).toThrow(FormatViolationError)
    })

    test('9-digit string (too short) throws FormatViolationError', () => {
      expect(() => validateBusinessRegistration('123456789')).toThrow(FormatViolationError)
    })

    test('11-digit string (too long) throws FormatViolationError', () => {
      expect(() => validateBusinessRegistration('12345678901')).toThrow(FormatViolationError)
    })
  })
})
